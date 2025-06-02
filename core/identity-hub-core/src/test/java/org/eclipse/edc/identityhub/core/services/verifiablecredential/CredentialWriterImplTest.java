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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CredentialWriterImplTest {

    public static final String TEST_CREDENTIAL_TYPE = "TestCredential";
    public static final String TEST_CREDENTIAL_FORMAT = CredentialFormat.VC1_0_JWT.toString();
    private static final String PARTICIPANT_ID = "participant";
    private final CredentialStore credentialStore = mock();
    private final TypeTransformerRegistry credentialTransformerRegistry = mock();
    private final HolderCredentialRequestStore holderCredentialRequestStore = mock();
    private final CredentialWriterImpl credentialWriter = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, new NoopTransactionContext(), JacksonJsonLd.createObjectMapper(), holderCredentialRequestStore);

    @BeforeEach
    void setUp() {
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid("did:web:issuer")
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .build()));
    }

    @Test
    void write() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isSucceeded();
        verify(holderCredentialRequestStore).save(argThat(request -> request.getIssuerPid() != null));
    }

    @Test
    void write_invalidFormat() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", "invalid-format")), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("Invalid format");
    }

    @Test
    void write_typeNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().types(List.of("NotRequestedCredential")).build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials of type");
    }

    @Test
    void write_formatNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", CredentialFormat.VC2_0_COSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials ");
    }

    @Test
    void write_storeFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }

    @Test
    void write_transformationFailure() {

        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.failure("foo"));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
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

        var result = credentialWriter.write("holderPid", "issuerPid", List.of(
                new CredentialWriteRequest("raw-cred1", TEST_CREDENTIAL_FORMAT),
                new CredentialWriteRequest("raw-cred2", CredentialFormat.VC2_0_JOSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail()
                .containsSequence("bar");
    }

    @Test
    void write_noHolderRequestFound_expectFailure() {
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.notFound("foo"));

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("foo");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    @Test
    void write_holderRequestInWrongState_expectFailure() {
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(HolderCredentialRequest.Builder.newInstance()
                .issuerDid("did:web:issuer")
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTING.code())
                .participantContextId(PARTICIPANT_ID)
                .build()));

        var result = credentialWriter.write("holderPid", "issuerPid", Set.of(new CredentialWriteRequest("raw-cred", TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed()
                .detail().startsWith("HolderCredentialRequest is expected to be in ");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    private VerifiableCredential.Builder createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(TEST_CREDENTIAL_TYPE))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build());
    }
}

