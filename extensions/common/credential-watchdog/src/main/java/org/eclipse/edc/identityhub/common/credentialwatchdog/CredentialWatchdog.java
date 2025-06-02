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

import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.EXPIRED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.NOT_YET_VALID;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.SUSPENDED;

/**
 * This is a runnable task that is intended to be executed periodically to fetch all non-expired, non-revoked credentials from storage, check for their status,
 * and update their status. Every execution (fetch-all - check-each - update-each) will run in a transaction.
 * <p>
 * Note that this will materialize <strong>all</strong> credentials into memory at once, as the general assumption is that typically, wallets don't
 * store an enormous amount of credentials. To mitigate this, the watchdog only considers credentials in states {@link VcStatus#EXPIRED}, {@link VcStatus#ISSUED},
 * {@link VcStatus#SUSPENDED} and {@link VcStatus#NOT_YET_VALID}, c.f. {@link CredentialWatchdog#ALLOWED_STATES}.
 *
 * <p>
 * Note also, that a credentials status will only be updated if it did in fact change, to avoid unnecessary database interactions.
 */
public class CredentialWatchdog implements Runnable {
    //todo: add more states once we have to check issuance status
    public static final List<Integer> ALLOWED_STATES = List.of(ISSUED.code(), NOT_YET_VALID.code(), SUSPENDED.code(), EXPIRED.code());
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
        transactionContext.execute(() -> {
            var allCredentials = credentialStore.query(allExcludingExpiredAndRevoked())
                    .onFailure(f -> monitor.warning("Failed to fetch credentials from database: %s".formatted(f.getFailureDetail())))
                    .orElse(f -> Collections.emptyList());

            monitor.debug("checking %d credentials".formatted(allCredentials.size()));

            // check status
            allCredentials.forEach(credential -> {
                var newStatus = credentialStatusCheckService.checkStatus(credential)
                        .orElse(f -> {
                            monitor.warning("Error determining status for credential '%s': %s. Will move to the ERROR state.".formatted(credential.getId(), f.getFailureDetail()));
                            return VcStatus.ERROR;
                        });
                var changed = credential.getState() != newStatus.code();
                if (changed) {
                    monitor.debug("Credential '%s' is now in status '%s'".formatted(credential.getId(), newStatus));
                    credential.setCredentialStatus(newStatus);
                    credentialStore.update(credential);
                }
            });

            // check credentials that are nearing expiry
            allCredentials.stream()
                    .filter(cred -> Instant.now().isAfter(cred.getVerifiableCredential().credential().getExpirationDate().minusSeconds(expiryGracePeriod.toSeconds())))
                    .forEach(this::startReissuance);
        });
    }

    private void startReissuance(VerifiableCredentialResource expiringCredential) {

        var formatString = expiringCredential.getVerifiableCredential().format().toString();
        var type = expiringCredential.getVerifiableCredential().credential().getType()
                .stream()
                .filter(s -> !s.equalsIgnoreCase("VerifiableCredential"))
                .findAny()
                .orElse(null);
        var credentialObjectId = ofNullable(expiringCredential.getMetadata().get("credentialObjectId")).map(Object::toString);

        if (credentialObjectId.isEmpty()) {
            monitor.warning("Attempting to start re-issuance for credential '%s' failed: No CredentialObjectId found (metadata property 'credentialObjectId'). Will abort re-issuance.".formatted(expiringCredential.getId()));
            return;
        }

        var requestedCredential = new RequestedCredential(credentialObjectId.get(), type, formatString);
        credentialRequestManager.initiateRequest(expiringCredential.getParticipantContextId(),
                        expiringCredential.getIssuerId(),
                        UUID.randomUUID().toString(),
                        List.of(requestedCredential))
                .onFailure(f -> monitor.warning("Error sending re-issuance request: %s".formatted(f.getFailureDetail())));
    }

    private QuerySpec allExcludingExpiredAndRevoked() {
        return QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "in", ALLOWED_STATES))
                .build();
    }
}
