/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.keypairs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.transit.TransitEngine;
import org.eclipse.edc.identityhub.transit.TransitKeyDescriptor;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext.API_TOKEN_ALIAS;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.CREDENTIAL_SIGNING;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.PRESENTATION_SIGNING;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.TOKEN_SIGNING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TransitKeyPairServiceTest {

    public static final String PARTICIPANT_ID = "test-participant";
    private final KeyPairResourceStore keyPairResourceStore = mock(i -> StoreResult.success());
    private final Vault vault = mock();
    private final KeyPairObservable observableMock = mock();
    private final ParticipantContextStore participantContextServiceMock = mock();
    private final TransitEngine transitEngine = mock();
    private final TransitKeyPairService keyPairService = new TransitKeyPairService(keyPairResourceStore, mock(), observableMock, new NoopTransactionContext(), participantContextServiceMock, transitEngine);


    @BeforeEach
    void setup() {
        when(participantContextServiceMock.query(any(QuerySpec.class)))
                .thenReturn(StoreResult.success(List.of(ParticipantContext.Builder.newInstance()
                        .participantContextId(PARTICIPANT_ID)
                        .identity("did:example:123")
                        .property(API_TOKEN_ALIAS, "apitoken-alias").build())));
    }

    private TransitKeyDescriptor transitKeyDescriptor() {
        try {
            return new ObjectMapper().readValue("""
                    {
                        "request_id": "test-request-id",
                        "mount_type": "transit",
                        "data": {
                            "name": "test-key",
                            "type": "ed25519",
                            "keys": {
                                "1": {
                                    "name": "ed25519",
                                    "public_key": "buwAsKUFAcRYbw5jXaR7Ay2NFidpJiTv3r9thttvgVc=",
                                    "creation_time": "2026-05-18T10:00:00Z"
                                }
                            },
                            "latest_version": 1,
                            "min_available_version": 0,
                            "min_decryption_version": 1,
                            "min_encryption_version": 0,
                            "exportable": false,
                            "deletion_allowed": false,
                            "supports_signing": true,
                            "supports_encryption": false,
                            "supports_decryption": false,
                            "supports_derivation": true
                        }
                    }
                    """, TransitKeyDescriptor.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyPairResource.Builder createKeyPairResource() {
        return KeyPairResource.Builder.newTokenSigning()
                .id(UUID.randomUUID().toString())
                .keyId("test-key-1")
                .privateKeyAlias("private-key-alias")
                .participantContextId(PARTICIPANT_ID)
                .serializedPublicKey("this-is-a-pem-string")
                .useDuration(Duration.ofDays(6).toMillis());
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance()
                .keyId("test-kid")
                .usage(Set.of(PRESENTATION_SIGNING))
                .privateKeyAlias("private-alias")
                .publicKeyJwk(null)
                .publicKeyPem(null);
    }

    private Map<String, Object> createJwk() {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .generate()
                    .toJSONObject();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class GetActiveKeyPair {
        @Test
        void getActiveKeyPairForUsage_singleKeyPair() {
            var keyPair = createKeyPairResource()
                    .usage(PRESENTATION_SIGNING)
                    .state(KeyPairState.ACTIVATED.code())
                    .build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, PRESENTATION_SIGNING);

            assertThat(result).isSucceeded()
                    .satisfies(kp -> assertThat(kp.getId()).isEqualTo(keyPair.getId()));
        }

        @Test
        void getActiveKeyPairForUsage_multipleKeyPairs_oneIsDefault() {
            var defaultKeyPair = createKeyPairResource()
                    .usage(CREDENTIAL_SIGNING)
                    .state(KeyPairState.ACTIVATED.code())
                    .isDefaultPair(true)
                    .build();

            var nonDefaultKeyPair = createKeyPairResource()
                    .usage(CREDENTIAL_SIGNING)
                    .state(KeyPairState.ACTIVATED.code())
                    .isDefaultPair(false)
                    .build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(defaultKeyPair, nonDefaultKeyPair)));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

            assertThat(result).isSucceeded()
                    .satisfies(kp -> assertThat(kp.getId()).isEqualTo(defaultKeyPair.getId()));
        }

        @Test
        void getActiveKeyPairForUsage_multipleKeyPairs_noneIsDefault() {
            var keyPair1 = createKeyPairResource()
                    .usage((TOKEN_SIGNING))
                    .state(KeyPairState.ACTIVATED.code())
                    .isDefaultPair(false)
                    .build();

            var keyPair2 = createKeyPairResource()
                    .usage((TOKEN_SIGNING))
                    .state(KeyPairState.ACTIVATED.code())
                    .isDefaultPair(false)
                    .build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair1, keyPair2)));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, TOKEN_SIGNING);

            assertThat(result).isFailed()
                    .detail().isEqualTo("Multiple key-pairs found for signing credentials, but none was marked as 'default'");
        }

        @Test
        void getActiveKeyPairForUsage_noMatchingKeyPairs() {
            var keyPair = createKeyPairResource()
                    .usage(Set.of(PRESENTATION_SIGNING))
                    .state(KeyPairState.ACTIVATED.code())
                    .build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

            assertThat(result).isFailed()
                    .detail().isEqualTo("No active key pair found for participant '%s' with usage 'CREDENTIAL_SIGNING'".formatted(PARTICIPANT_ID));
        }

        @Test
        void getActiveKeyPairForUsage_storeQueryFails() {
            when(keyPairResourceStore.query(any())).thenReturn(StoreResult.notFound("Store error"));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, TOKEN_SIGNING);

            assertThat(result).isFailed()
                    .detail().contains("Error obtaining private key for participant '%s'".formatted(PARTICIPANT_ID));
        }

        @Test
        void getActiveKeyPairForUsage_keyPairWithMultipleUsages() {
            var keyPair = createKeyPairResource()
                    .usage(Set.of(PRESENTATION_SIGNING, CREDENTIAL_SIGNING))
                    .state(KeyPairState.ACTIVATED.code())
                    .build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

            var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

            assertThat(result).isSucceeded()
                    .satisfies(kp -> assertThat(kp.getId()).isEqualTo(keyPair.getId()));
        }
    }

    @Nested
    class Activate {
        @ParameterizedTest(name = "Valid state = {0}")
        // cannot use enum literals and the .code() method -> needs to be compile constant
        @ValueSource(ints = { 100, 200 })
        void activate(int validState) {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).state(validState).build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(keyPairResourceStore.update(any())).thenReturn(success());

            assertThat(keyPairService.activate(oldId)).isSucceeded();
        }

        @ParameterizedTest(name = "Valid state = {0}")
        // cannot use enum literals and the .code() method -> needs to be compile constant
        @ValueSource(ints = { 0, 30, 400, -10 })
        void activate_invalidState(int validState) {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).state(validState).build();

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(keyPairResourceStore.update(any())).thenReturn(success());

            assertThat(keyPairService.activate(oldId))
                    .isFailed()
                    .detail()
                    .isEqualTo("The key pair resource is expected to be in [200, 100], but was %s".formatted(validState));
        }

        @Test
        void activate_notExists() {

            when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));

            assertThat(keyPairService.activate("notexists"))
                    .isFailed()
                    .detail()
                    .isEqualTo("A KeyPairResource with ID 'notexists' does not exist.");
        }
    }

    @Nested
    class RevokeKey {

        @Test
        void revokeKey_notfound() {
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));

            var newKey = createKey().build();

            assertThat(keyPairService.revokeKey("not-exist", newKey)).isFailed()
                    .detail().isEqualTo("A KeyPairResource with ID 'not-exist' does not exist.");

            verify(keyPairResourceStore).query(any());
            verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
        }

        @Test
        void revokeKey() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinAvailableVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinDecryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isSucceeded();

            verify(keyPairResourceStore).update(argThat(kpr -> kpr.getState() == KeyPairState.REVOKED.code()));
            verify(keyPairResourceStore).create(any());
            verify(transitEngine).rotateKey(anyString());
            verify(transitEngine).getKey(anyString());
            verify(transitEngine).setMinAvailableVersion(anyString(), anyInt());
            verify(transitEngine).setMinEncryptionKeyVersion(anyString(), anyInt());
            verify(transitEngine).setMinDecryptionKeyVersion(anyString(), anyInt());
            verify(observableMock).invokeForEach(any());
            verifyNoMoreInteractions(observableMock, transitEngine);
        }

        @Test
        void revokeKey_whenStoreUpdateFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(keyPairResourceStore.update(any())).thenReturn(StoreResult.generalError("update failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(keyPairResourceStore).update(any());
            verifyNoMoreInteractions(observableMock, transitEngine);
        }

        @Test
        void revokeKey_whenRotateFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.failure("rotate failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(transitEngine).rotateKey(anyString());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void revokeKey_whenGetKeyFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.failure("getKey failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(transitEngine).rotateKey(anyString());
            verify(transitEngine).getKey(anyString());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void revokeKey_whenSetMinAvailableVersionFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinDecryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinAvailableVersion(anyString(), anyInt())).thenReturn(Result.failure("setMinAvailable failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(transitEngine).setMinAvailableVersion(anyString(), anyInt());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void revokeKey_whenSetMinEncryptionVersionFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinAvailableVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.failure("setMinEncryption failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(transitEngine).setMinEncryptionKeyVersion(anyString(), anyInt());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void revokeKey_whenSetMinDecryptionVersionFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinAvailableVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinDecryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.failure("setMinDecryption failed"));

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(transitEngine).setMinDecryptionKeyVersion(anyString(), anyInt());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void revokeKey_whenStoreCreateFails_shouldFail() {
            var oldKey = createKeyPairResource().build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(keyPairResourceStore.create(any())).thenReturn(StoreResult.generalError("create failed"));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinAvailableVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());
            when(transitEngine.setMinDecryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());

            assertThat(keyPairService.revokeKey(oldKey.getId(), null)).isFailed();

            verify(keyPairResourceStore).create(any());
            verifyNoMoreInteractions(observableMock);
        }
    }

    @Nested
    class RotateKeyPair {
        @Test
        void rotateKeyPair() {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).build();

            assertThat(Arrays.asList(createKey().keyGeneratorParams(null).build(),
                    createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(Map.of(
                            "algorithm", "EdDSA",
                            "curve", "Ed25519"
                    )).build(),
                    null))
                    .allSatisfy(kd -> {
                        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
                        when(keyPairResourceStore.create(any())).thenReturn(success());
                        when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
                        when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
                        when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.success());

                        assertThat(keyPairService.rotateKeyPair(oldId, kd, Duration.ofDays(100).toMillis())).isSucceeded();

                        verify(keyPairResourceStore).query(any());
                        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
                        verify(keyPairResourceStore).create(any());
                        verify(transitEngine).rotateKey(anyString());
                        verify(transitEngine).getKey(anyString());
                        verify(transitEngine).setMinEncryptionKeyVersion(anyString(), anyInt());
                        verify(observableMock, times(1)).invokeForEach(any()); // 1 for rotate
                        verifyNoMoreInteractions(keyPairResourceStore, observableMock, transitEngine);
                        reset(keyPairResourceStore, observableMock, transitEngine);
                    });
        }

        @Test
        void rotateKeyPair_oldKeyNotFound() {
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));
            when(keyPairResourceStore.create(any())).thenReturn(success());

            var newKey = createKey().build();

            assertThat(keyPairService.rotateKeyPair("not-exist", newKey, Duration.ofDays(100).toMillis())).isFailed()
                    .detail().isEqualTo("A KeyPairResource with ID 'not-exist' does not exist.");

            verify(keyPairResourceStore).query(any());
            verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
        }

        @Test
        void rotateKeyPair_whenStoreUpdateFails_shouldFail() {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(keyPairResourceStore.update(any())).thenReturn(StoreResult.generalError("update failed"));

            assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isFailed();

            verify(keyPairResourceStore).update(any());
            verifyNoMoreInteractions(observableMock, transitEngine);
        }

        @Test
        void rotateKeyPair_whenRotateFails_shouldFail() {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.failure("rotate failed"));

            assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isFailed();

            verify(transitEngine).rotateKey(anyString());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void rotateKeyPair_whenGetKeyFails_shouldFail() {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.failure("getKey failed"));

            assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isFailed();

            verify(transitEngine).rotateKey(anyString());
            verify(transitEngine).getKey(anyString());
            verifyNoMoreInteractions(observableMock);
        }

        @Test
        void rotateKeyPair_whenSetMinEncryptionVersionFails_shouldFail() {
            var oldId = "old-id";
            var oldKey = createKeyPairResource().id(oldId).build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
            when(transitEngine.rotateKey(anyString())).thenReturn(Result.success());
            when(transitEngine.getKey(anyString())).thenReturn(Result.success(transitKeyDescriptor()));
            when(transitEngine.setMinEncryptionKeyVersion(anyString(), anyInt())).thenReturn(Result.failure("setMinEncryption failed"));

            assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isFailed();

            verify(transitEngine).setMinEncryptionKeyVersion(anyString(), anyInt());
            verifyNoMoreInteractions(observableMock);
        }
    }

    @Nested
    class AddKeyPair {
        // Transit does indeed support importing externally generated keys, but our implementation does not (yet?) support it
        @Test
        void addKeyPair_publicKeyGiven_expectFailure() {

            when(keyPairResourceStore.create(any())).thenReturn(success());
            var key = createKey().publicKeyJwk(createJwk()).publicKeyPem(null).keyGeneratorParams(null).build();

            assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isFailed().detail()
                    .matches(".*Importing .* is not supported.");

            verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
        }

        @ParameterizedTest
        @ValueSource(strings = { "ed25519", "ecdsa-p256", "rsa-4096" })
        void addKeyPair_shouldGenerate_storesInTransit(String keyType) {
            when(keyPairResourceStore.create(any())).thenReturn(success());
            when(transitEngine.generateKey(any(), eq(keyType))).thenReturn(Result.success(transitKeyDescriptor()));

            var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                    "type", keyType
            )).build();

            assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isSucceeded();

            verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() &&
                    kpr.getParticipantContextId().equals(PARTICIPANT_ID) &&
                    kpr.getState() == KeyPairState.ACTIVATED.code()));
            // new key is set to active - expect an update in the DB
            verify(transitEngine).generateKey(anyString(), eq(keyType));
            verify(observableMock, times(1)).invokeForEach(any());
            verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
        }

        @Test
        void addKeyPair_participantNotFound() {
            when(participantContextServiceMock.query(any(QuerySpec.class))).thenReturn(StoreResult.success(List.of()));
            assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, createKey().build(), false)).isFailed()
                    .detail().isEqualTo("No ParticipantContext with ID '%s' was found.".formatted(PARTICIPANT_ID));
        }

        @Test
        void addKeyPair_whenParticipantDeactivated_shouldFail() {
            var pc = ParticipantContext.Builder.newInstance()
                    .participantContextId(PARTICIPANT_ID)
                    .identity("did:example:123")
                    .property(API_TOKEN_ALIAS, "apitoken-alias")
                    .state(ParticipantContextState.DEACTIVATED)
                    .build();
            when(participantContextServiceMock.query(any(QuerySpec.class))).thenReturn(StoreResult.success(List.of(pc)));

            assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, createKey().build(), false))
                    .isFailed()
                    .detail()
                    .isEqualTo("To add a key pair, the ParticipantContext with ID '%s' must be in state ACTIVATED or CREATED but was DEACTIVATED.".formatted(PARTICIPANT_ID));
        }
    }

    @Nested
    class OnEvent {

        @Test
        void onParticipantContextDeleted_deletesAllKeyPairsAndTransitKeys() {
            var kp1 = createKeyPairResource().id("kp-1").privateKeyAlias("alias-1").build();
            var kp2 = createKeyPairResource().id("kp-2").privateKeyAlias("alias-2").build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(kp1, kp2)));
            when(transitEngine.deleteKey(anyString())).thenReturn(Result.success());

            keyPairService.on(deletedEvent());

            verify(keyPairResourceStore).deleteById("kp-1");
            verify(keyPairResourceStore).deleteById("kp-2");
            verify(transitEngine).deleteKey("alias-1");
            verify(transitEngine).deleteKey("alias-2");
        }

        @Test
        void onParticipantContextDeleted_whenStoreQueryFails_doesNothing() {
            when(keyPairResourceStore.query(any())).thenReturn(StoreResult.generalError("query failed"));

            keyPairService.on(deletedEvent());

            verify(keyPairResourceStore).query(any());
            verifyNoMoreInteractions(keyPairResourceStore, transitEngine);
        }

        @Test
        void onParticipantContextDeleted_whenStoreDeleteFails_logsWarning() {
            var kp1 = createKeyPairResource().id("kp-1").privateKeyAlias("alias-1").build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(kp1)));
            when(keyPairResourceStore.deleteById(anyString())).thenReturn(StoreResult.generalError("delete failed"));

            keyPairService.on(deletedEvent());

            verify(keyPairResourceStore).deleteById("kp-1");
            verifyNoMoreInteractions(transitEngine);
        }

        @Test
        void onParticipantContextDeleted_whenTransitDeleteFails_continuesAndLogs() {
            var kp1 = createKeyPairResource().id("kp-1").privateKeyAlias("alias-1").build();
            var kp2 = createKeyPairResource().id("kp-2").privateKeyAlias("alias-2").build();
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of(kp1, kp2)));
            when(transitEngine.deleteKey("alias-1")).thenReturn(Result.failure("vault error"));
            when(transitEngine.deleteKey("alias-2")).thenReturn(Result.success());

            keyPairService.on(deletedEvent());

            verify(keyPairResourceStore).deleteById("kp-1");
            verify(keyPairResourceStore).deleteById("kp-2");
            verify(transitEngine).deleteKey("alias-1");
            verify(transitEngine).deleteKey("alias-2");
            verifyNoMoreInteractions(transitEngine);
        }

        @Test
        void onParticipantContextDeleted_whenNoKeyPairs_doesNothing() {
            when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));

            keyPairService.on(deletedEvent());

            verify(keyPairResourceStore).query(any());
            verifyNoMoreInteractions(keyPairResourceStore, transitEngine);
        }

        @Test
        void onOtherEvent_shouldDoNothing() {
            //noinspection unchecked
            keyPairService.on(EventEnvelope.Builder.newInstance().at(System.currentTimeMillis()).payload(KeyPairAdded.Builder.newInstance().participantContextId(PARTICIPANT_ID).build()).build());
            verifyNoInteractions(keyPairResourceStore, transitEngine);
        }

        @SuppressWarnings("unchecked")
        private EventEnvelope<ParticipantContextDeleted> deletedEvent() {
            return EventEnvelope.Builder.newInstance()
                    .payload(ParticipantContextDeleted.Builder.newInstance()
                            .participantContextId(TransitKeyPairServiceTest.PARTICIPANT_ID)
                            .build())
                    .at(System.currentTimeMillis())
                    .id(UUID.randomUUID().toString())
                    .build();
        }
    }
}