/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.store.QueryPages;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialEntry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.BITSTRING_STATUS_LIST_PREFIX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.BITSTRING_ENCODED_LIST_LITERAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.BITSTRING_STATUSLIST_CREDENTIAL;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.BITSTRING_STATUS_LIST;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.REVOCATION;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.VERIFIABLE_CREDENTIAL;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;

public class BitstringStatusListManager implements StatusListManager {

    /**
     * the length of the bitstring in the status list credential, i.e. its number of entries
     */
    public static final String BITSTRING_SIZE = "bitstringSize";
    private static final int PAGE_SIZE = 100;
    private final CredentialStore store;
    private final TransactionContext transactionContext;
    private final CredentialGeneratorRegistry credentialGenerator;
    private final IdentityHubParticipantContextService participantContextService;
    private final StatusListCredentialPublisher publisher;
    private final BitstringStatusListConfiguration configuration;
    private final Clock clock;
    private final Monitor monitor;

    public BitstringStatusListManager(CredentialStore store,
                                      TransactionContext transactionContext,
                                      CredentialGeneratorRegistry credentialGenerator,
                                      IdentityHubParticipantContextService participantContextService,
                                      StatusListCredentialPublisher publisher,
                                      BitstringStatusListConfiguration configuration,
                                      Clock clock,
                                      Monitor monitor) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.credentialGenerator = credentialGenerator;
        this.participantContextService = participantContextService;
        this.publisher = publisher;
        this.configuration = configuration;
        this.clock = clock;
        this.monitor = monitor;
    }

    @Override
    public ServiceResult<StatusListCredentialEntry> getActiveCredential(String participantContextId) {
        return transactionContext.execute(() -> {
            var usableCredential = findUsableStatusListCredential(participantContextId);
            if (usableCredential.succeeded() && usableCredential.getContent().isEmpty()) {
                // the query may have waited for the lock of another runtime, which has since filled up the status list and
                // created a new one. A query that was already under way cannot see that new status list, but a new query can,
                // so look again before creating yet another one
                usableCredential = findUsableStatusListCredential(participantContextId);
            }
            if (usableCredential.failed()) {
                return usableCredential.mapFailure();
            }

            // obtain the current index, current credential by ID and its published URL
            return usableCredential.getContent()
                    .map(ServiceResult::success)
                    .orElseGet(() -> createNewStatusListCredential(participantContextId))
                    .map(cred -> new BitstringStatusListCredentialEntry(statusListIndex(cred), cred, publicUri(cred)));
        });
    }

    @Override
    public ServiceResult<Void> incrementIndex(StatusListCredentialEntry entry) {
        return transactionContext.execute(() -> {
            var updatedCredential = entry.statusListCredential().toBuilder()
                    .metadata(new HashMap<>(entry.statusListCredential().getMetadata()))
                    .metadata(CURRENT_INDEX, entry.statusListIndex() + 1)
                    .metadata(PUBLIC_URL, entry.credentialUrl())
                    .build();

            return upsert(updatedCredential);
        });
    }

    /**
     * Signs a status list credential again: it is valid for the configured validity from now on, and a status list that
     * is shorter than the configured length is extended to it. Existing entries keep their indexes. Only the bitstring,
     * the dates and the {@link #BITSTRING_SIZE} change, so the status list credential keeps its ID and URL.
     * <p>
     * The given resource is left unchanged if signing fails.
     */
    @Override
    public ServiceResult<VerifiableCredentialResource> renew(VerifiableCredentialResource statusListCredential) {
        var container = statusListCredential.getVerifiableCredential();
        var bitString = decode(statusListCredential);
        if (bitString.failed()) {
            return ServiceResult.unexpected(bitString.getFailureDetail());
        }
        var extendedBitString = extend(bitString.getContent());
        var encodedList = BitString.Writer.newInstance().writeMultibase(extendedBitString);
        if (encodedList.failed()) {
            return ServiceResult.unexpected(encodedList.getFailureDetail());
        }

        var renewedCredential = renewedCopy(container.credential(), encodedList.getContent());
        var signed = credentialGenerator.signCredential(statusListCredential.getParticipantContextId(), renewedCredential, container.format());
        if (signed.failed()) {
            return ServiceResult.unexpected(signed.getFailureDetail());
        }
        var metadata = new HashMap<>(statusListCredential.getMetadata());
        metadata.put(BITSTRING_SIZE, extendedBitString.length());
        return ServiceResult.success(statusListCredential.toBuilder()
                .credential(signed.getContent())
                .metadata(metadata)
                .build());
    }

    /**
     * Renews the status list credentials of all participant contexts that expire within the given time, so that they do
     * not expire while nothing else changes them. Each one is locked while it is renewed, and skipped if another runtime
     * renewed it in the meantime. A status list credential that cannot be renewed is skipped, and attempted again on the
     * next call.
     */
    public void renewExpiring(Duration within) {
        var deadline = clock.instant().plus(within);
        var expiring = new ArrayList<StatusListKey>();
        QueryPages.forEachPage(allBitstringStatusLists(), PAGE_SIZE, this::query, VerifiableCredentialResource::getId,
                page -> page.stream().filter(statusList -> expiresBefore(statusList, deadline))
                        .forEach(statusList -> expiring.add(new StatusListKey(statusList.getParticipantContextId(), statusList.getId()))));

        for (var statusList : expiring) {
            try {
                transactionContext.execute(() -> renewIfExpiring(statusList, deadline));
            } catch (RuntimeException e) {
                monitor.warning("Could not renew status list credential '%s'".formatted(statusList.id()), e);
            }
        }
    }

    /**
     * Looks up the status list credential that new holder credentials can be added to, i.e. an active one that is not full.
     * All status list credentials of the participant context stay locked until the surrounding transaction completes, so
     * that no other runtime can hand out the same index before this one has been incremented, c.f. {@link StatusListManager}
     */
    private ServiceResult<Optional<VerifiableCredentialResource>> findUsableStatusListCredential(String participantContextId) {
        var credentialQueryResult = store.queryForUpdate(whereTypeIsBitstringCredential(participantContextId));
        if (credentialQueryResult.failed()) {
            return ServiceResult.fromFailure(credentialQueryResult);
        }
        var statusListCredentials = new ArrayList<VerifiableCredentialResource>();
        for (var statusListCredential : credentialQueryResult.getContent()) {
            var extended = extendIfShorter(statusListCredential);
            if (extended.failed()) {
                return extended.mapFailure();
            }
            statusListCredentials.add(extended.getContent());
        }
        // a status list credential that was full may have been extended, and is used again before newer ones
        var usable = statusListCredentials.stream()
                .filter(this::isActive)
                .filter(this::isNotFull)
                .findFirst();
        if (usable.isEmpty()) {
            return ServiceResult.success(Optional.empty());
        }
        return renewIfExpired(usable.get()).map(Optional::of);
    }

    /**
     * Extends a status list that is shorter than the configured length, e.g. one that was created before the length was
     * raised, c.f. {@link #renewOrKeep(VerifiableCredentialResource, String)}.
     */
    private ServiceResult<VerifiableCredentialResource> extendIfShorter(VerifiableCredentialResource statusListCredential) {
        if (capacity(statusListCredential) >= configuration.bitstringSize()) {
            return ServiceResult.success(statusListCredential);
        }
        return renewOrKeep(statusListCredential, "extend");
    }

    /**
     * Renews the status list credential that new holder credentials are added to if it has expired, e.g. because the
     * periodic renewal is deactivated or failed, so that their status can be verified, c.f.
     * {@link #renewOrKeep(VerifiableCredentialResource, String)}. Other expired status list credentials are left to the
     * periodic renewal.
     */
    private ServiceResult<VerifiableCredentialResource> renewIfExpired(VerifiableCredentialResource statusListCredential) {
        if (!expiresBefore(statusListCredential, clock.instant())) {
            return ServiceResult.success(statusListCredential);
        }
        return renewOrKeep(statusListCredential, "renew");
    }

    /**
     * Renews a status list credential, publishes it again and stores it, so that it is only stored once it was published.
     * If it cannot be signed or published, e.g. because the vault or the publication target is not available for a
     * moment, it is used as it is, and renewed the next time: issuing a holder credential must not fail because of it.
     *
     * @param action what the renewal does to the status list credential, for the warning
     */
    private ServiceResult<VerifiableCredentialResource> renewOrKeep(VerifiableCredentialResource statusListCredential, String action) {
        var renewed = renewAndPublish(statusListCredential);
        if (renewed.failed()) {
            monitor.warning("Could not %s status list credential '%s', it is used as it is: %s"
                    .formatted(action, statusListCredential.getId(), renewed.getFailureDetail()));
            return ServiceResult.success(statusListCredential);
        }
        var updateResult = store.update(renewed.getContent());
        if (updateResult.failed()) {
            return ServiceResult.fromFailure(updateResult);
        }
        return renewed;
    }

    private void renewIfExpiring(StatusListKey statusList, Instant deadline) {
        var locked = store.queryForUpdate(QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantContextId", "=", statusList.participantContextId()))
                .filter(new Criterion("id", "=", statusList.id()))
                .build());
        if (locked.failed() || locked.getContent().isEmpty()) {
            return;
        }
        var statusListCredential = locked.getContent().iterator().next();
        if (!expiresBefore(statusListCredential, deadline)) {
            // renewed by another runtime in the meantime
            return;
        }
        // not stored if it cannot be published, so that it is renewed again on the next call
        var renewed = renewAndPublish(statusListCredential);
        if (renewed.failed()) {
            monitor.warning("Could not renew status list credential '%s': %s".formatted(statusList.id(), renewed.getFailureDetail()));
            return;
        }
        store.update(renewed.getContent())
                .onFailure(f -> monitor.warning("Could not store the renewed status list credential '%s': %s".formatted(statusList.id(), f.getFailureDetail())));
    }

    private boolean expiresBefore(VerifiableCredentialResource statusListCredential, Instant deadline) {
        var expirationDate = statusListCredential.getVerifiableCredential().credential().getExpirationDate();
        return expirationDate != null && expirationDate.isBefore(deadline);
    }

    private Collection<VerifiableCredentialResource> query(QuerySpec query) {
        return store.query(query).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    /**
     * Copies a status list credential with the given bitstring, valid for the configured validity from now on.
     */
    @SuppressWarnings("unchecked")
    private VerifiableCredential renewedCopy(VerifiableCredential credential, String encodedList) {
        var subjects = credential.getCredentialSubject().stream()
                .map(subject -> CredentialSubject.Builder.newInstance()
                        .id(subject.getId())
                        .claims(new HashMap<>(subject.getClaims()))
                        .claim(BITSTRING_ENCODED_LIST_LITERAL, encodedList)
                        .build())
                .toList();
        // status list credentials that were created before were missing the type that every credential must have
        var types = new ArrayList<>(credential.getType());
        if (!types.contains(VERIFIABLE_CREDENTIAL)) {
            types.add(0, VERIFIABLE_CREDENTIAL);
        }
        var now = clock.instant();
        return VerifiableCredential.Builder.newInstance()
                .id(credential.getId())
                .contexts(new ArrayList<>(credential.getContext()))
                .types(types)
                .issuer(credential.getIssuer())
                .name(credential.getName())
                .description(credential.getDescription())
                .credentialSubjects(new ArrayList<>(subjects))
                .credentialStatus(new ArrayList<>(credential.getCredentialStatus()))
                .credentialSchemas(new ArrayList<>(credential.getCredentialSchema()))
                .dataModelVersion(credential.getDataModelVersion())
                .issuanceDate(now)
                .expirationDate(now.plus(configuration.validity()))
                .build();
    }

    /**
     * Returns the given bitstring if it has the configured length or more, or a copy of it with the configured length.
     */
    private BitString extend(BitString bitString) {
        if (bitString.length() >= configuration.bitstringSize()) {
            return bitString;
        }
        var extended = BitString.Builder.newInstance().size(configuration.bitstringSize()).build();
        for (var index = 0; index < bitString.length(); index++) {
            if (bitString.get(index)) {
                extended.set(index, true);
            }
        }
        return extended;
    }

    private Result<BitString> decode(VerifiableCredentialResource statusListCredential) {
        var credential = statusListCredential.getVerifiableCredential().credential();
        var encodedList = credential.getCredentialSubject().stream()
                .map(subject -> subject.getClaim(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_ENCODED_LIST_LITERAL))
                .filter(Objects::nonNull)
                .findFirst();
        if (encodedList.isEmpty()) {
            return Result.failure("Status list credential '%s' has no encoded list".formatted(statusListCredential.getId()));
        }
        return BitString.Parser.newInstance().parse(encodedList.get().toString());
    }

    /**
     * The number of entries of a status list, as recorded when it was created or extended, or else as given by its
     * bitstring.
     */
    private int capacity(VerifiableCredentialResource statusListCredential) {
        return ofNullable(statusListCredential.getMetadata().get(BITSTRING_SIZE))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElseGet(() -> decode(statusListCredential).map(BitString::length).orElse(f -> 0));
    }

    /**
     * inserts or updates ("up-serts") a credential resource, specifically a status list credential
     *
     * @param statusListCredential the status list credential to insert/update
     */
    private ServiceResult<Void> upsert(VerifiableCredentialResource statusListCredential) {

        var res = store.update(statusListCredential);
        if (res.failed() && res.reason() == StoreFailure.Reason.NOT_FOUND) {
            return ServiceResult.from(store.create(statusListCredential));
        }
        return ServiceResult.from(res);
    }

    @SuppressWarnings("unchecked")
    private ServiceResult<VerifiableCredentialResource> createNewStatusListCredential(String participantContextId) {

        var participant = participantContextService.getParticipantContext(participantContextId);
        if (participant.failed()) {
            return participant.mapFailure();
        }
        var participantDid = participant.getContent().getDid();

        // generate new bitstring status credential. The W3C Bitstring Status List builds on the VC Data Model 2.0
        var now = clock.instant();

        var credential = VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("type", BITSTRING_STATUS_LIST)
                        .claim("statusPurpose", REVOCATION)
                        .claim("encodedList", createEmptyBitstring())
                        //todo: add support for statusMessage, statusSize, etc.
                        .build())
                .id(UUID.randomUUID().toString())
                .context(VC_PREFIX_V2)
                .dataModelVersion(DataModelVersion.V_2_0)
                .issuanceDate(now)
                .expirationDate(now.plus(configuration.validity()))
                .issuer(new Issuer(participantDid))
                .types(List.of(VERIFIABLE_CREDENTIAL, BITSTRING_STATUSLIST_CREDENTIAL))
                .build();

        // sign and package in resource
        //do not upsert - this must be an insert
        return credentialGenerator.signCredential(participantContextId, credential, CredentialFormat.VC2_0_JOSE)
                .map(signedCredential -> createCredentialResource(participantContextId, signedCredential, participantDid))
                .compose(this::storeResource)
                .compose(this::publish)
                .flatMap(ServiceResult::from);
    }

    private Result<VerifiableCredentialResource> storeResource(VerifiableCredentialResource res) {
        var createResult = store.create(res);
        return createResult.succeeded() ? Result.success(res) : Result.failure(createResult.getFailureDetail());
    }

    private VerifiableCredentialResource createCredentialResource(String participantContextId, VerifiableCredentialContainer signedCredential, String issuerDid) {
        return VerifiableCredentialResource.Builder.newStatusList()
                .id(UUID.randomUUID().toString())
                .state(VcStatus.ISSUED)
                .metadata(Map.of(CURRENT_INDEX, 0,
                        IS_ACTIVE, true,
                        BITSTRING_SIZE, configuration.bitstringSize()))
                .participantContextId(participantContextId)
                .credential(signedCredential)
                .issuerId(issuerDid)
                .holderId(issuerDid)
                .build();
    }

    private Result<VerifiableCredentialResource> publish(VerifiableCredentialResource statusListCredential) {
        return publisher.publish(statusListCredential)
                .compose(url -> {
                    var meta = new HashMap<>(statusListCredential.getMetadata());
                    meta.put("published", true);
                    meta.put(PUBLIC_URL, url);
                    var updated = statusListCredential.toBuilder().metadata(meta).build();
                    var storeResult = upsert(updated);
                    return storeResult.succeeded() ?
                            Result.success(updated) : Result.failure(storeResult.getFailureDetail());
                });
    }

    /**
     * Renews a status list credential, c.f. {@link #renew(VerifiableCredentialResource)}, and publishes it again. It keeps
     * the URL it was published at first, because the holder credentials that have an entry on it refer to that URL.
     * <p>
     * The given resource is left unchanged if signing or publishing fails, so that it can still be used as it is.
     */
    private ServiceResult<VerifiableCredentialResource> renewAndPublish(VerifiableCredentialResource statusListCredential) {
        // renewing changes the given resource itself, c.f. VerifiableCredentialResource#toBuilder
        var originalCredential = statusListCredential.getVerifiableCredential();
        var originalMetadata = statusListCredential.getMetadata();
        var renewed = renew(statusListCredential);
        if (renewed.failed()) {
            return renewed;
        }
        var published = publisher.publish(renewed.getContent());
        if (published.failed()) {
            statusListCredential.toBuilder().credential(originalCredential).metadata(originalMetadata).build();
            return ServiceResult.unexpected("Could not publish status list credential '%s': %s"
                    .formatted(statusListCredential.getId(), published.getFailureDetail()));
        }
        return renewed;
    }

    private String createEmptyBitstring() {
        var bs = BitString.Builder.newInstance()
                .size(configuration.bitstringSize())
                .build();
        return BitString.Writer.newInstance().writeMultibase(bs).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private @Nullable String publicUri(VerifiableCredentialResource res) {
        return ofNullable(res.getMetadata().get(PUBLIC_URL)).map(Object::toString).orElse(null);
    }

    private Integer statusListIndex(VerifiableCredentialResource statusListCredential) {
        return ofNullable(statusListCredential.getMetadata().get(CURRENT_INDEX)).map(Object::toString).map(Integer::parseInt).orElse(0);
    }

    /**
     * An issuer can have multiple status list credentials, but only one can be the "active" one, i.e. the one where new holder
     * credentials get added.
     */
    private boolean isActive(VerifiableCredentialResource res) {
        return Boolean.TRUE.equals(res.getMetadata().get(IS_ACTIVE));
    }

    /**
     * computes whether a status list credential is "full", i.e. if all the bits are occupied. This is done by comparing the
     * length of the bitstring and the current index. If the current status list index is equal (or larger than) the bitstring
     * size, the credential is considered "full", and a new one needs to be created
     */
    private boolean isFull(VerifiableCredentialResource statusListCredential) {
        return capacity(statusListCredential) <= statusListIndex(statusListCredential);
    }

    private boolean isNotFull(VerifiableCredentialResource statusListCredential) {
        return !isFull(statusListCredential);
    }

    private QuerySpec allBitstringStatusLists() {
        return QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.type", "=", BITSTRING_STATUS_LIST))
                .filter(new Criterion("usage", "=", CredentialUsage.StatusList.toString()))
                .build();
    }

    private QuerySpec whereTypeIsBitstringCredential(String participantContextId) {
        return QuerySpec.Builder.newInstance()
                .filter(filterByParticipantContextId(participantContextId))
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.type", "=", BITSTRING_STATUS_LIST))
                .filter(new Criterion("usage", "=", CredentialUsage.StatusList.toString()))
                // oldest first: the status list credentials are locked in this order, so a runtime that waits for a lock
                // does so on a status list that existed before, without holding the lock of a newer one. Otherwise it could
                // deadlock with a runtime that holds the older ones and is about to use a newer one, c.f. getActiveCredential
                .sortField("timestamp")
                .sortOrder(SortOrder.ASC)
                // all of them, not only as many as by default: otherwise the active one could be missed, and a new one
                // created on every call
                .limit(Integer.MAX_VALUE)
                .build();
    }

    /**
     * Identifies a status list credential, whose ID is only unique within its participant context.
     */
    private record StatusListKey(String participantContextId, String id) {
    }
}
