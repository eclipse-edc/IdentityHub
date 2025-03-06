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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialEntry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.BITSTRING_STATUS_LIST;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.REVOCATION;

public class BitstringStatusListManager implements StatusListManager {
    /**
     * marks the "active" credential, i.e. the ones where new holder credentials get added
     */
    private static final String IS_ACTIVE = "isActive";
    /**
     * the current status list index. needed to detect overflow or "fullness"s
     */
    private static final String CURRENT_INDEX = "currentIndex";
    /**
     * the public URL where the status list credential can be obtained
     */
    private static final String PUBLIC_URL = "publicUrl";
    /**
     * the size of the bitstring in the status list credential. Defaults to 16kB.
     */
    private static final String BITSTRING_SIZE = "bitstringSize";
    private static final int DEFAULT_BITSTRING_SIZE = 16 * 1024;
    private final CredentialStore store;
    private final TransactionContext transactionContext;
    private final CredentialGeneratorRegistry credentialGenerator;
    private final ParticipantContextService participantContextService;

    public BitstringStatusListManager(CredentialStore store, TransactionContext transactionContext, CredentialGeneratorRegistry credentialGenerator, ParticipantContextService participantContextService) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.credentialGenerator = credentialGenerator;
        this.participantContextService = participantContextService;
    }

    @Override
    public ServiceResult<StatusListCredentialEntry> getActiveCredential(String participantContextId) {
        return transactionContext.execute(() -> {
            var queryRes = store.query(whereTypeIsBitstringCredential(participantContextId));
            if (queryRes.failed()) {
                return ServiceResult.fromFailure(queryRes);
            }

            var bitStringCredentials = queryRes.getContent();

            // obtain the current index, current credential by ID and its published URL
            var slCred = bitStringCredentials.stream()
                    .filter(this::isActive)
                    .filter(this::isNotFull)
                    .findFirst()
                    .map(Result::success)
                    .orElseGet(() -> createNewStatusListCredential(participantContextId));

            return ServiceResult.from(slCred)
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


    private Result<VerifiableCredentialResource> createNewStatusListCredential(String participantContextId) {

        var participant = participantContextService.getParticipantContext(participantContextId);
        if (participant.failed()) {
            return Result.failure(participant.getFailureDetail());
        }
        var participantDid = participant.getContent().getDid();

        // generate new bitstring status credential
        var now = Instant.now();

        var credential = VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("type", BITSTRING_STATUS_LIST)
                        .claim("statusPurpose", REVOCATION)
                        .claim("encodedList", createEmptyBitstring())
                        //todo: add support for statusMessage, statusSize, etc.
                        .build())
                .issuanceDate(now)
                .expirationDate(now.plus(365, ChronoUnit.DAYS)) //todo: make configurable
                .issuer(new Issuer(participantDid))
                .type(BITSTRING_STATUS_LIST)
                .build();

        // sign and package in resource
        return credentialGenerator.signCredential(participantContextId, credential, CredentialFormat.VC1_0_JWT)
                .map(signedCredential -> VerifiableCredentialResource.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .state(VcStatus.ISSUED)
                        .metadata(Map.of(CURRENT_INDEX, 0,
                                IS_ACTIVE, true,
                                PUBLIC_URL, "http://foo.bar",  // todo: wait for publish result
                                BITSTRING_SIZE, DEFAULT_BITSTRING_SIZE)) //todo: make configurable
                        .participantContextId(participantContextId)
                        .credential(signedCredential)
                        .issuerId(participantDid)
                        .holderId(participantDid)
                        .build());
    }

    // creates an empty 16kb bitstring:
    private String createEmptyBitstring() {
        var bs = BitString.Builder.newInstance()
                .size(DEFAULT_BITSTRING_SIZE) //todo: make configurable
                .build();
        return BitString.Writer.newInstance().encoder(Base64.getUrlEncoder()).write(bs).orElseThrow(f -> new EdcException(f.getFailureDetail()));
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
        var index = statusListIndex(statusListCredential);
        return ofNullable(statusListCredential.getMetadata().getOrDefault(BITSTRING_SIZE, DEFAULT_BITSTRING_SIZE))
                .map(Object::toString)
                .map(Integer::parseInt)
                .map(bitstringSize -> bitstringSize <= index)
                .orElse(true);
    }

    private boolean isNotFull(VerifiableCredentialResource statusListCredential) {
        return !isFull(statusListCredential);
    }

    private QuerySpec whereTypeIsBitstringCredential(String participantContextId) {
        return QuerySpec.Builder.newInstance()
                .filter(ParticipantResource.filterByParticipantContextId(participantContextId))
                .filter(new Criterion("credential.credentialSubject.type", "=", BITSTRING_STATUS_LIST))
                .build();
    }
}
