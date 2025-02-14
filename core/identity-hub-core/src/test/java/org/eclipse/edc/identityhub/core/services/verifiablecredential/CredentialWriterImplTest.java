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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CredentialWriterImplTest {

    private static final String PARTICIPANT_ID = "participant";
    private final CredentialStore credentialStore = mock();
    private final TypeTransformerRegistry credentialTransformerRegistry = mock();
    private final CredentialWriterImpl credentialWriter = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, new NoopTransactionContext(), JacksonJsonLd.createObjectMapper());

    @Test
    void write() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write(Set.of(new CredentialWriteRequest("raw-cred", CredentialFormat.VC1_0_JWT.toString())), PARTICIPANT_ID);
        assertThat(result).isSucceeded();
    }

    @Test
    void write_invalidFormat() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write(Set.of(new CredentialWriteRequest("raw-cred", "invalid-format")), PARTICIPANT_ID);
        assertThat(result).isFailed()
                .detail().contains("Invalid format");
    }

    @Test
    void write_storeFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var result = credentialWriter.write(Set.of(new CredentialWriteRequest("raw-cred", CredentialFormat.VC1_0_JWT.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }

    @Test
    void write_transformationFailure() {

        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.failure("foo"));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write(Set.of(new CredentialWriteRequest("raw-cred", CredentialFormat.VC1_0_JWT.toString())), PARTICIPANT_ID);
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

        var result = credentialWriter.write(List.of(
                new CredentialWriteRequest("raw-cred1", CredentialFormat.VC1_0_JWT.toString()),
                new CredentialWriteRequest("raw-cred2", CredentialFormat.VC2_0_JOSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail()
                .containsSequence("bar");
    }

    private VerifiableCredential.Builder createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("test-types"))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build());
    }
}

