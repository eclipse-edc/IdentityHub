/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.common.credentialwatchdog;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ERROR;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.EXPIRED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.NOT_YET_VALID;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REQUESTED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.SUSPENDED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_SUPERSEDED_BY;
import static org.eclipse.edc.identityhub.store.QueryPages.forEachPage;

/**
 * This is a runnable task that is intended to be executed periodically to fetch all non-expired, non-revoked credentials from storage, check for their status,
 * and update their status. Each credential is checked and updated in a transaction of its own, so that a failure only affects that
 * credential. A failed execution does not keep later executions from running.
 * <p>
 * The credentials are fetched and checked page by page, so that all of them are checked, however many there are, without
 * holding all of them in memory at once. The watchdog only considers credentials in states {@link VcStatus#EXPIRED}, {@link VcStatus#ISSUED},
 * {@link VcStatus#SUSPENDED}, {@link VcStatus#NOT_YET_VALID}, {@link VcStatus#REQUESTED} and {@link VcStatus#ERROR}, c.f. {@link CredentialWatchdog#ALLOWED_STATES}.
 *
 * <p>
 * Note also, that a credentials status will only be updated if it did in fact change, to avoid unnecessary database interactions.
 */
public class CredentialWatchdog implements Runnable {
    private static final int PAGE_SIZE = 100;
    //todo: add more states once we have to check issuance status
    // REQUESTED marks a credential whose renewal is in flight. It is fetched so that a renewal which ended without
    // delivering a replacement can be noticed and the credential released again, c.f. #reconcileRenewal
    // ERROR marks a credential whose status could not be determined, e.g. because its status list was unreachable. It is
    // fetched so that it is checked again, and recovers once the check succeeds, instead of never being looked at again.
    public static final List<Integer> ALLOWED_STATES = List.of(ISSUED.code(), NOT_YET_VALID.code(), SUSPENDED.code(), EXPIRED.code(), REQUESTED.code(), ERROR.code());
    private static final List<HolderRequestState> PENDING_REQUEST_STATES = List.of(HolderRequestState.CREATED, HolderRequestState.REQUESTING, HolderRequestState.REQUESTED);
    private final CredentialStore credentialStore;
    private final CredentialStatusCheckService credentialStatusCheckService;
    private final Monitor monitor;
    private final TransactionContext transactionContext;
    private final Duration expiryGracePeriod;
    private final CredentialRequestManager credentialRequestManager;

    public CredentialWatchdog(CredentialStore credentialStore,
                              CredentialStatusCheckService credentialStatusCheckService,
                              Monitor monitor,
                              TransactionContext transactionContext,
                              Duration expiryGracePeriod,
                              CredentialRequestManager credentialRequestManager) {
        this.credentialStore = credentialStore;
        this.credentialStatusCheckService = credentialStatusCheckService;
        this.monitor = monitor;
        this.transactionContext = transactionContext;
        this.expiryGracePeriod = expiryGracePeriod;
        this.credentialRequestManager = credentialRequestManager;
    }

    @Override
    public void run() {
        // the watchdog runs on a schedule, which stops for good once a run throws an exception, so none may escape
        try {
            forEachPage(allExcludingExpiredAndRevoked(), PAGE_SIZE, this::fetchCredentials, VerifiableCredentialResource::getId, credentials -> {
                monitor.debug("checking %d credentials".formatted(credentials.size()));
                credentials.forEach(this::check);
            });
        } catch (Exception e) {
            monitor.severe("The credential watchdog failed, it runs again in its next period", e);
        }
    }

    private Collection<VerifiableCredentialResource> fetchCredentials(QuerySpec query) {
        return transactionContext.execute(() -> credentialStore.query(query))
                .onFailure(f -> monitor.warning("Failed to fetch credentials from database: %s".formatted(f.getFailureDetail())))
                .orElse(f -> Collections.emptyList());
    }

    /**
     * Checks a single credential in transactions of its own, so that a failure only affects that credential, and the
     * other ones are checked all the same.
     * <p>
     * Determining the status may download the credential's status list, so it happens outside of a transaction, in order
     * not to hold a database connection, nor any locks, while waiting for it. The changes before and after it are made in
     * a short transaction each.
     * <p>
     * Every runtime runs the watchdog, so the same credential may be checked by several of them at the same time, and a
     * delivery may supersede it meanwhile. Each transaction therefore reads the credential anew and locks it, and leaves
     * it for the next run if it changed while its status was determined. Otherwise, it could be renewed twice, or a
     * superseded credential could be made usable again.
     */
    private void check(VerifiableCredentialResource credential) {
        try {
            var checked = transactionContext.execute(() -> {
                var current = lock(credential.getId());
                // credentials waiting on a renewal are held back: their REQUESTED state records that fact, and the status
                // check below would overwrite it. Those whose renewal came to nothing are released here and treated normally.
                return current != null && reconcileRenewal(current) ? current : null;
            });
            if (checked == null) {
                return;
            }
            var newStatus = determineStatus(checked);
            transactionContext.execute(() -> {
                var current = lock(credential.getId());
                if (current == null || hasChanged(checked, current)) {
                    monitor.debug("Credential '%s' changed while its status was determined, it is checked again in the next run".formatted(credential.getId()));
                    return;
                }
                updateStatus(current, newStatus);
                if (isDueForRenewal(current)) {
                    startReissuance(current);
                }
            });
        } catch (Exception e) {
            monitor.warning("The credential watchdog failed to check credential '%s': %s".formatted(credential.getId(), e.getMessage()), e);
        }
    }

    /**
     * Reads the credential, and locks it until the transaction completes.
     *
     * @return the credential, or null if it does not exist anymore, or could not be read
     */
    private @Nullable VerifiableCredentialResource lock(String credentialId) {
        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", credentialId))
                .build();
        return credentialStore.queryForUpdate(query)
                .onFailure(f -> monitor.warning("Failed to read credential '%s': %s".formatted(credentialId, f.getFailureDetail())))
                .map(credentials -> credentials.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }

    /**
     * Whether the credential was changed since it was read, in a way that matters to the watchdog: e.g. another runtime
     * started its renewal or updated its status, or a delivery superseded it.
     */
    private boolean hasChanged(VerifiableCredentialResource before, VerifiableCredentialResource after) {
        return before.getState() != after.getState() ||
                !Objects.equals(before.getMetadata().get(METADATA_SUPERSEDED_BY), after.getMetadata().get(METADATA_SUPERSEDED_BY)) ||
                !Objects.equals(before.getMetadata().get(METADATA_RENEWAL_REQUEST_ID), after.getMetadata().get(METADATA_RENEWAL_REQUEST_ID));
    }

    private VcStatus determineStatus(VerifiableCredentialResource credential) {
        return credentialStatusCheckService.checkStatus(credential)
                .orElse(f -> {
                    monitor.warning("Error determining status for credential '%s': %s. Will move to the ERROR state.".formatted(credential.getId(), f.getFailureDetail()));
                    return VcStatus.ERROR;
                });
    }

    private void updateStatus(VerifiableCredentialResource credential, VcStatus newStatus) {
        var changed = credential.getState() != newStatus.code();
        if (changed) {
            monitor.debug("Credential '%s' is now in status '%s'".formatted(credential.getId(), newStatus));
            credential.setCredentialStatus(newStatus);
            credentialStore.update(credential);
        }
    }

    /**
     * Whether re-issuance should be initiated for the credential, because it is nearing (or past) expiry, unless a
     * replacement credential was already issued: the state alone cannot express that distinction, because EXPIRED covers
     * both a superseded credential and one that ran out without a replacement. The latter must still be renewed, while
     * renewing a superseded one would loop forever, as every delivery expires its predecessor. A credential without an
     * expiration date never needs renewal.
     */
    private boolean isDueForRenewal(VerifiableCredentialResource credential) {
        var expirationDate = credential.getVerifiableCredential().credential().getExpirationDate();
        return !credential.isSuperseded() &&
                expirationDate != null &&
                Instant.now().isAfter(expirationDate.minusSeconds(expiryGracePeriod.toSeconds()));
    }

    /**
     * Decides whether a credential may be acted upon in this run, and recovers those whose renewal led nowhere.
     * <p>
     * A credential in {@link VcStatus#REQUESTED} is waiting for the renewal request it is linked to via
     * {@link VerifiableCredentialResource#METADATA_RENEWAL_REQUEST_ID}. While that request is still under way the
     * credential is left exactly as it is. Once the request is done without a replacement having been delivered - it
     * failed, or it is no longer on record - the credential is released: staying in {@link VcStatus#REQUESTED} would keep
     * it out of {@link CredentialWatchdog#ALLOWED_STATES} forever, so neither its status nor its expiry would ever be
     * looked at again. The reason is recorded on the credential, and a later run may renew it anew.
     *
     * @return true if the credential is not waiting on a renewal, i.e. its status and expiry may be acted upon
     */
    private boolean reconcileRenewal(VerifiableCredentialResource credential) {
        if (credential.getStateAsEnum() != VcStatus.REQUESTED) {
            return true;
        }

        var requestId = ofNullable(credential.getMetadata().get(VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID))
                .map(Object::toString)
                .orElse(null);
        var request = requestId == null ? null : credentialRequestManager.findById(requestId);

        if (request != null && PENDING_REQUEST_STATES.contains(request.stateAsEnum())) {
            monitor.debug("Credential '%s' is waiting for renewal request '%s', which is in state '%s'"
                    .formatted(credential.getId(), requestId, request.stateAsString()));
            return false;
        }

        if (request == null) {
            var reason = "the renewal request '%s' is no longer on record".formatted(requestId);
            credential.getMetadata().put(VerifiableCredentialResource.METADATA_RENEWAL_ERROR, reason);
            monitor.warning("Renewal of credential '%s' cannot be tracked, because %s. Releasing it for another attempt."
                    .formatted(credential.getId(), reason));
        } else if (request.stateAsEnum() == HolderRequestState.ERROR) {
            credential.getMetadata().put(VerifiableCredentialResource.METADATA_RENEWAL_ERROR, request.getErrorDetail());
            monitor.warning("Renewal of credential '%s' failed: %s. Releasing it for another attempt."
                    .formatted(credential.getId(), request.getErrorDetail()));
        } else {
            // the request was fulfilled, but this credential was not superseded by what arrived, so it is simply released
            credential.getMetadata().remove(VerifiableCredentialResource.METADATA_RENEWAL_ERROR);
        }

        credential.getMetadata().remove(VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID);
        credentialStore.update(credential);
        return true;
    }

    private void startReissuance(VerifiableCredentialResource expiringCredential) {

        var formatString = expiringCredential.getVerifiableCredential().format().toString();
        var type = expiringCredential.getVerifiableCredential().credential().getType()
                .stream()
                .filter(s -> !s.equalsIgnoreCase("VerifiableCredential"))
                .findAny()
                .orElse(null);
        var credentialObjectId = ofNullable(expiringCredential.getMetadata().get(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID)).map(Object::toString);

        if (credentialObjectId.isEmpty()) {
            monitor.warning("Attempting to start re-issuance for credential '%s' failed: No CredentialObjectId found (metadata property '%s'). Will abort re-issuance."
                    .formatted(expiringCredential.getId(), VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID));
            return;
        }

        var requestedCredential = new RequestedCredential(credentialObjectId.get(), type, formatString);

        credentialRequestManager.initiateRequest(expiringCredential.getParticipantContextId(),
                        expiringCredential.getIssuerId(),
                        UUID.randomUUID().toString(),
                        List.of(requestedCredential))
                .compose(holderRequestId -> {
                    // the credential is parked in REQUESTED for as long as that request runs, and linked to it, so that
                    // a renewal which never delivers a replacement can be recognized on a later run
                    expiringCredential.getMetadata().put(VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID, holderRequestId);
                    expiringCredential.getMetadata().remove(VerifiableCredentialResource.METADATA_RENEWAL_ERROR);
                    expiringCredential.setCredentialStatus(VcStatus.REQUESTED);
                    return ServiceResult.from(credentialStore.update(expiringCredential));
                })
                .onFailure(f -> monitor.warning("Error sending re-issuance request: %s".formatted(f.getFailureDetail())));
    }

    private QuerySpec allExcludingExpiredAndRevoked() {
        return QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "in", ALLOWED_STATES))
                .filter(new Criterion("usage", "=", CredentialUsage.Holder.toString()))
                .build();
    }
}
