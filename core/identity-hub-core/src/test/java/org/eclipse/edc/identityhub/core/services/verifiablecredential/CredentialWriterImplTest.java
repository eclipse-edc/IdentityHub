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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CredentialWriterImplTest {

    public static final String TEST_CREDENTIAL_TYPE = "TestCredential";
    public static final String TEST_CREDENTIAL_FORMAT = CredentialFormat.VC1_0_JWT.toString();
    private static final String PARTICIPANT_ID = "participant";
    private static final String ISSUER_DID = "did:web:issuer";
    private static final String HOLDER_DID = "did:web:holder";
    private final CredentialStore credentialStore = mock();
    private final TypeTransformerRegistry credentialTransformerRegistry = mock();
    private final HolderCredentialRequestStore holderCredentialRequestStore = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final DidPublicKeyResolver publicKeyResolver = mock();
    private final Monitor monitor = mock();
    private final CredentialWriterImpl credentialWriter = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, new NoopTransactionContext(),
            JacksonJsonLd.createObjectMapper(), holderCredentialRequestStore, tokenValidationService, publicKeyResolver, monitor);

    @BeforeEach
    void setUp() {
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList())).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .build()));
    }

    @Test
    @DisplayName("CS-STOR-01: a valid CredentialMessage stores the credential and completes the request")
    void write() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isSucceeded();
        verify(holderCredentialRequestStore).save(argThat(request -> request.getIssuerPid() != null));
    }

    @Test
    void write_invalidFormat() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", "invalid-format")), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("Invalid format");
    }

    @Test
    @DisplayName("CS-STOR-08: a credential type that was not requested is rejected and nothing is stored")
    void write_typeNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().types(List.of("NotRequestedCredential")).build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials of type");
    }

    @Test
    @DisplayName("CS-STOR-08: a credential format that was not requested is rejected and nothing is stored")
    void write_formatNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", CredentialFormat.VC2_0_COSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials ");
    }

    @Test
    void write_storeFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }

    @Test
    void write_transformationFailure() {

        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.failure("foo"));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }

    @Test
    void write_multipleFail_expectSingleFailures() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()))
                .thenReturn(Result.failure("foo"));

        when(credentialStore.create(any()))
                .thenReturn(StoreResult.alreadyExists("bar"))
                .thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, List.of(
                new CredentialWriteRequest("raw-cred1", TEST_CREDENTIAL_FORMAT),
                new CredentialWriteRequest("raw-cred2", CredentialFormat.VC2_0_JOSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail()
                .containsSequence("bar");
    }

    @Test
    @DisplayName("CS-STOR-07: a holderPid matching no pending request is rejected and nothing is stored")
    void write_noHolderRequestFound_expectFailure() {
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.notFound("foo"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("foo");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    @Test
    void write_holderRequestInWrongState_expectFailure() {
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTING.code())
                .participantContextId(PARTICIPANT_ID)
                .build()));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed()
                .detail().startsWith("HolderCredentialRequest is expected to be in ");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    // CS-STOR-06: status=ISSUED with empty credentials list -> accepted as no-op: success, nothing stored, holder request state unchanged
    @Test
    @DisplayName("CS-STOR-06: an ISSUED message with an empty credentials list is a no-op and leaves the request state unchanged")
    void write_emptyCredentials_expectNoOpSuccess() {
        // default fixture (setUp()) returns a request in state REQUESTED

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(), PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
        // the request must not be transitioned to ISSUED when no credentials were delivered
        verify(holderCredentialRequestStore, never()).save(argThat(request -> request.getState() == ISSUED.code()));
    }

    // CS-STOR-07: message issuerPid differs from the issuerPid already stored on the HolderCredentialRequest -> rejected, not silently overwritten
    @Test
    @DisplayName("CS-STOR-07: a message whose issuerPid differs from the one stored on the request is rejected")
    void write_issuerPidMismatch_expectFailure() {
        // the stored request already carries a different issuerPid
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("stored-issuer-pid")
                .build()));
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "a-different-issuer-pid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed();
        verify(holderCredentialRequestStore, never()).save(argThat(request -> "a-different-issuer-pid".equals(request.getIssuerPid())));
    }

    // CS-STOR-07: cross-tenant — holderPid belongs to a request of participant B, but write() is called with participantContextId A -> not-found/unauthorized, nothing stored
    @Test
    @DisplayName("CS-STOR-07: a holderPid belonging to another participant context is rejected and nothing is stored")
    void write_holderRequestBelongsToOtherParticipantContext_expectFailure() {
        // the stored request belongs to participant B
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId("participant-b")
                .build()));
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        // write is invoked for participant A - the lookup must be scoped to that participant context
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        // rejected (not-found/unauthorized), nothing stored, no existence leak across tenants
        assertThat(result).isFailed();
        verify(credentialStore, never()).create(any());
    }

    // CS-STOR-13: exact re-delivery of the same credential to a request already in ISSUED -> no-op success, credentialStore.create not called again, no duplicate
    @Test
    @DisplayName("CS-STOR-13: re-delivery of the same credential to an ISSUED request is an idempotent no-op without duplicate storage")
    void write_redeliveryToIssuedRequest_expectIdempotentNoOp() {
        // the request is already in state ISSUED
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(ISSUED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build()));
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        // the very same credential is delivered a second time
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        // accepted, but no duplicate is written
        assertThat(result).isSucceeded();
        verify(credentialStore, never()).create(any());
        // the existing credential request is not modified, no storage interaction
        verify(holderCredentialRequestStore, never()).save(any());
    }

    // A3.25 / CS-STOR-05: a REJECTED CredentialMessage fails the pending request instead of leaving it waiting forever
    @Test
    @DisplayName("CS-STOR-05: a rejection from the request's issuer moves the request to ERROR and stores nothing")
    void reject_pendingRequest_expectTransitionToError() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "attestation not satisfied", PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(HolderRequestState.ERROR);
        Assertions.assertThat(request.getErrorDetail()).contains("attestation not satisfied");
        verify(holderCredentialRequestStore).save(request);
        verifyNoInteractions(credentialStore);
    }

    @Test
    @DisplayName("CS-STOR-05: a rejection without a reason still fails the request")
    void reject_withoutReason_expectTransitionToError() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, null, PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(HolderRequestState.ERROR);
        Assertions.assertThat(request.getErrorDetail()).contains("issuerPid");
    }

    @Test
    @DisplayName("CS-STOR-09: a rejection from an issuer other than the one addressed is not accepted")
    void reject_fromDifferentIssuer_expectUnauthorized() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));

        var result = credentialWriter.reject("holderPid", "issuerPid", "did:web:someone-else", "nope", PARTICIPANT_ID);

        assertThat(result).isFailed();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(REQUESTED);
        verify(holderCredentialRequestStore, never()).save(any());
        verify(holderCredentialRequestStore).breakLease(request);
    }

    @Test
    @DisplayName("CS-STOR-13: a rejection trailing a completed issuance is acknowledged but does not undo it")
    void reject_afterCredentialsIssued_expectNoOp() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(ISSUED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "too late", PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(ISSUED);
        verify(holderCredentialRequestStore, never()).save(any());
        verify(holderCredentialRequestStore).breakLease(request);
    }

    @Test
    @DisplayName("CS-STOR-05: a rejection for another participant context's request is not observable")
    void reject_crossTenant_expectNotFound() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId("another-participant")
                .issuerPid("issuerPid")
                .build();
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "nope", PARTICIPANT_ID);

        assertThat(result).isFailed();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(REQUESTED);
        verify(holderCredentialRequestStore).breakLease(request);
    }

    private VerifiableCredential.Builder createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(TEST_CREDENTIAL_TYPE))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id(HOLDER_DID).claim("test-claim", "test-value").build());
    }
}

