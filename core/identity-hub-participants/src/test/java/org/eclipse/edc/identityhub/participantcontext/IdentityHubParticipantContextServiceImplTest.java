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

package org.eclipse.edc.identityhub.participantcontext;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountCredentials;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.transaction.TrackingTransactionContext;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class IdentityHubParticipantContextServiceImplTest {

    private final Vault vault = mock();
    private final ParticipantContextStore participantContextStore = mock();
    private final ParticipantContextObservable observableMock = mock();
    private final DidResourceStore didResourceStore = mock();
    private final StsAccountProvisioner stsAccountProvisioner = mock();
    private final ParticipantContextConfigService configService = mock();
    private final Monitor monitor = mock();
    private final TrackingTransactionContext transactionContext = new TrackingTransactionContext();
    private IdentityHubParticipantContextServiceImpl participantContextService;

    @BeforeEach
    void setUp() {
        var keyParserRegistry = new KeyParserRegistryImpl();
        keyParserRegistry.register(new PemParser(mock()));
        participantContextService = new IdentityHubParticipantContextServiceImpl(participantContextStore, didResourceStore, vault, transactionContext, observableMock, stsAccountProvisioner, configService, monitor);
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.success());
        when(vault.deleteSecret(anyString(), anyString())).thenReturn(Result.success());
        when(configService.save(any(ParticipantContextConfiguration.class))).thenReturn(ServiceResult.success());
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withPublicKeyPem(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());

        var pem = """
                -----BEGIN PUBLIC KEY-----
                MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE25DvKuU5+gvMdKkyiDDIsx3tcuPX
                jgVyAjs1JcfFtvi9I0FemuqymDTu3WWdYmdaJQMJJx3qwEJGTVTxcKGtEg==
                -----END PUBLIC KEY-----
                """;

        var ctx = createManifest()
                .active(isActive)
                .key(createKey()
                        .publicKeyJwk(null)
                        .publicKeyPem(pem)
                        .build()).build();

        assertThat(participantContextService.createParticipantContext(ctx)).isSucceeded().satisfies(response -> {
            assertThat(response.apiKey()).isNotBlank();
            assertThat(response.clientId()).isNull();
            assertThat(response.clientSecret()).isNull();
        });

        verify(participantContextStore).create(any());
        verify(vault).storeSecret(anyString(), eq(ctx.getParticipantContextId() + "-apikey"), anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
        verify(observableMock).invokeForEach(any());
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void shouldCreateParticipantContext_withAccountInfo(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.success(new AccountCredentials("clientId", "clientSecret")));

        var pem = """
                -----BEGIN PUBLIC KEY-----
                MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE25DvKuU5+gvMdKkyiDDIsx3tcuPX
                jgVyAjs1JcfFtvi9I0FemuqymDTu3WWdYmdaJQMJJx3qwEJGTVTxcKGtEg==
                -----END PUBLIC KEY-----
                """;

        var ctx = createManifest()
                .active(isActive)
                .key(createKey()
                        .publicKeyJwk(null)
                        .publicKeyPem(pem)
                        .build()).build();

        var result = participantContextService.createParticipantContext(ctx);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.apiKey()).isNotBlank();
            assertThat(response.clientId()).isEqualTo("clientId");
            assertThat(response.clientSecret()).isEqualTo("clientSecret");
        });
    }

    @Test
    void shouldSkipStsProvisioning_whenProvisionStsAccountIsFalse() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());

        var ctx = createManifest()
                .provisionStsAccount(false)
                .build();

        var result = participantContextService.createParticipantContext(ctx);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.apiKey()).isNotBlank();
            assertThat(response.clientId()).isNull();
            assertThat(response.clientSecret()).isNull();
        });
        verify(stsAccountProvisioner, never()).create(any());
    }

    @Test
    void shouldSkipApiKeyProvisioning_whenProvisionApiKeyIsFalse() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());

        var ctx = createManifest()
                .provisionApiKey(false)
                .build();

        var result = participantContextService.createParticipantContext(ctx);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.apiKey()).isNull();
        });
        verify(vault, never()).storeSecret(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withPublicKeyJwk(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());

        var ctx = createManifest().active(isActive)
                .build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(argThat(pc -> pc.getIdentity() != null &&
                pc.getParticipantContextId().equalsIgnoreCase("test-id")));
        verify(vault).storeSecret(anyString(), eq(ctx.getParticipantContextId() + "-apikey"), anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
        verify(observableMock).invokeForEach(any());
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withKeyGenParams(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        var ctx = createManifest()
                .active(isActive)
                .key(createKey().publicKeyPem(null).publicKeyJwk(null)
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "ed25519"))
                        .build())
                .build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verify(vault).storeSecret(anyString(), eq(ctx.getParticipantContextId() + "-apikey"), anyString());

        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @Test
    void createParticipantContext_storageFails() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.duplicateKeys("foobar"));
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());

        var ctx = createManifest().build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed();

        verify(participantContextStore).create(any());
        verify(configService, never()).save(any(ParticipantContextConfiguration.class));
        verifyNoMoreInteractions(vault, participantContextStore, observableMock);
    }

    @Test
    void createParticipantContext_whenExists() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.alreadyExists("test-failure"));

        var ctx = createManifest().build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(participantContextStore).create(any());
        // the configuration is saved with an upsert, so it would replace that of the existing participant context
        verify(configService, never()).save(any(ParticipantContextConfiguration.class));
        verifyNoMoreInteractions(vault, participantContextStore, observableMock);

    }

    @Test
    void createParticipantContext_whenDidExists() {
        var ctx = createManifest().build();
        when(didResourceStore.findById(anyString())).thenReturn(DidResource.Builder.newInstance().did(ctx.getDid()).build());

        assertThat(participantContextService.createParticipantContext(ctx)).isFailed()
                .detail().isEqualTo("Another participant with the same DID '%s' already exists.".formatted(ctx.getDid()));

        verify(didResourceStore).findById(eq(ctx.getDid()));
        verifyNoMoreInteractions(didResourceStore, participantContextStore, observableMock);
    }

    @Test
    void createParticipantContext_whenConfigurationFails_shouldRollBack() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(configService.save(any(ParticipantContextConfiguration.class))).thenReturn(ServiceResult.unexpected("foobar"));

        assertThat(participantContextService.createParticipantContext(createManifest().build())).isFailed()
                .detail().isEqualTo("foobar");

        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault, never()).storeSecret(anyString(), anyString(), anyString());
        verifyNoInteractions(stsAccountProvisioner, observableMock);
    }

    @Test
    void createParticipantContext_whenApiTokenFails_shouldRollBack() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.failure("foobar"));

        assertThat(participantContextService.createParticipantContext(createManifest().build())).isFailed()
                .detail().contains("foobar");

        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault, never()).deleteSecret(anyString(), anyString());
        verifyNoInteractions(stsAccountProvisioner, observableMock);
    }

    @Test
    void createParticipantContext_whenStsAccountFails_shouldRollBackAndDeleteApiToken() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.unexpected("foobar"));
        var manifest = createManifest().build();

        assertThat(participantContextService.createParticipantContext(manifest)).isFailed()
                .detail().isEqualTo("foobar");

        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault).deleteSecret("test-id", "test-id-apikey");
        verify(vault, never()).deleteSecret("test-id", manifest.clientSecretAlias());
        verifyNoInteractions(observableMock);
    }

    @Test
    void createParticipantContext_whenProvisioningFails_shouldRollBackAndDeleteSecrets() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.success(new AccountCredentials("clientId", "clientSecret")));
        doThrow(new ProvisioningException(ServiceResult.conflict("foobar"))).when(observableMock).invokeForEach(any());
        var manifest = createManifest().build();

        assertThat(participantContextService.createParticipantContext(manifest)).isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT))
                .detail().isEqualTo("foobar");

        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault).deleteSecret("test-id", "test-id-apikey");
        verify(vault).deleteSecret("test-id", manifest.clientSecretAlias());
    }

    @Test
    void createParticipantContext_whenUnexpectedExceptionIsThrown_shouldDeleteSecretsAndRethrow() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.success(new AccountCredentials("clientId", "clientSecret")));
        var exception = new EdcException("foobar");
        doThrow(exception).when(observableMock).invokeForEach(any());
        var manifest = createManifest().build();

        assertThatThrownBy(() -> participantContextService.createParticipantContext(manifest)).isSameAs(exception);

        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault).deleteSecret("test-id", "test-id-apikey");
        verify(vault).deleteSecret("test-id", manifest.clientSecretAlias());
    }

    @Test
    void createParticipantContext_whenDeletingSecretFails_shouldWarn() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(vault.deleteSecret(anyString(), anyString())).thenReturn(Result.failure("vault down"));
        doThrow(new ProvisioningException(ServiceResult.conflict("foobar"))).when(observableMock).invokeForEach(any());

        assertThat(participantContextService.createParticipantContext(createManifest().provisionStsAccount(false).build())).isFailed()
                .detail().isEqualTo("foobar");

        verify(monitor).warning(contains("test-id-apikey"));
    }

    @Test
    void createParticipantContext_withApiKeyAlias() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(stsAccountProvisioner.create(any())).thenReturn(ServiceResult.success(new AccountCredentials("clientId", "clientSecret")));

        var pem = """
                -----BEGIN PUBLIC KEY-----
                MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE25DvKuU5+gvMdKkyiDDIsx3tcuPX
                jgVyAjs1JcfFtvi9I0FemuqymDTu3WWdYmdaJQMJJx3qwEJGTVTxcKGtEg==
                -----END PUBLIC KEY-----
                """;

        var ctx = createManifest()
                .active(true)
                .apiKeyAlias("test-alias")
                .key(createKey()
                        .publicKeyJwk(null)
                        .publicKeyPem(pem)
                        .build()).build();

        var result = participantContextService.createParticipantContext(ctx);

        assertThat(result).isSucceeded().satisfies(response -> {
            assertThat(response.apiKey()).isNotBlank();
            assertThat(response.clientId()).isEqualTo("clientId");
            assertThat(response.clientSecret()).isEqualTo("clientSecret");
        });
        verify(vault).storeSecret(anyString(), eq("test-alias"), anyString());
    }

    @Test
    void getParticipantContext() {
        var ctx = createContext();
        when(participantContextStore.findById(any())).thenReturn(StoreResult.success(ctx));

        assertThat(participantContextService.getParticipantContext("test-id"))
                .isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(ctx);

        verify(participantContextStore).findById(anyString());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void getParticipantContext_whenNotExists() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.notFound("foo"));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    assertThat(f.getFailureDetail()).isEqualTo("foo");
                });

        verify(participantContextStore).findById(anyString());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void getParticipantContext_whenStorageFails() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.notFound("foo bar"));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).findById(anyString());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void deleteParticipantContext() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createContext()));
        when(participantContextStore.findByIdForUpdate(anyString())).thenReturn(StoreResult.success(createContext()));
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());
        assertThat(participantContextService.deleteParticipantContext("test-id")).isSucceeded();

        verify(participantContextStore).deleteById(anyString());
        verify(observableMock, times(3)).invokeForEach(any());
        verify(vault).deleteSecret(anyString(), anyString());
        verifyNoMoreInteractions(vault, observableMock);
    }


    @Test
    void deleteParticipantContext_whenNotExists() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createContext()));
        when(participantContextStore.findByIdForUpdate(anyString())).thenReturn(StoreResult.success(createContext()));
        when(participantContextStore.deleteById(any())).thenReturn(StoreResult.notFound("foo bar"));
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());

        assertThat(participantContextService.deleteParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(observableMock, times(2)).invokeForEach(any()); //deleting
        verify(participantContextStore).deleteById(anyString());
        verify(vault).deleteSecret(anyString(), anyString());
        verifyNoMoreInteractions(vault, observableMock);
    }

    @Test
    void regenerateApiToken() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createContext()));
        when(vault.storeSecret(anyString(), eq("test-alias"), anyString())).thenReturn(Result.success());

        assertThat(participantContextService.regenerateApiToken("test-id")).isSucceeded().isNotNull();

        verify(participantContextStore).findById(anyString());
        verify(vault).storeSecret(anyString(), eq("test-alias"), argThat(s -> s.length() >= 64));
    }

    @Test
    void regenerateApiToken_vaultFails() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.success(createContext()));
        when(vault.storeSecret(anyString(), eq("test-alias"), anyString())).thenReturn(Result.failure("test failure"));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("Could not store new API token: test failure.");

        verify(participantContextStore).findById(anyString());
        verify(vault).storeSecret(anyString(), eq("test-alias"), anyString());
    }

    @Test
    void regenerateApiToken_whenNotFound() {
        when(participantContextStore.findById(anyString())).thenReturn(StoreResult.notFound("foo"));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("foo");

        verify(participantContextStore).findById(anyString());
        verifyNoMoreInteractions(participantContextStore, vault);
    }

    @Test
    void update() {
        var context = createContext();
        var transactions = new ArrayList<Integer>();
        when(participantContextStore.findByIdForUpdate(anyString())).thenAnswer(i -> {
            transactions.add(transactionContext.currentTransaction());
            return StoreResult.success(context);
        });
        when(participantContextStore.update(any())).thenAnswer(i -> {
            transactions.add(transactionContext.currentTransaction());
            return StoreResult.success();
        });
        assertThat(participantContextService.updateParticipant(context.getParticipantContextId(), IdentityHubParticipantContext::deactivate)).isSucceeded();

        // the participant context is locked when it is read, until it is written in the same transaction
        assertThat(transactions).containsExactly(1, 1);
        verify(participantContextStore, never()).findById(anyString());
        verify(participantContextStore).update(any());
        verify(observableMock).invokeForEach(any());
    }

    @Test
    void update_whenNotFound() {
        var context = createContext();
        when(participantContextStore.findByIdForUpdate(anyString())).thenReturn(StoreResult.notFound("foobar"));
        assertThat(participantContextService.updateParticipant(context.getParticipantContextId(), IdentityHubParticipantContext::deactivate)).isFailed()
                .detail().isEqualTo("ParticipantContext with ID 'test-id' not found.");

        verify(participantContextStore).findByIdForUpdate(anyString());
        verifyNoMoreInteractions(participantContextStore, observableMock);
    }

    @Test
    void update_whenStoreUpdateFails() {
        var context = createContext();
        when(participantContextStore.findByIdForUpdate(anyString())).thenReturn(StoreResult.success(context));
        when(participantContextStore.update(any())).thenReturn(StoreResult.alreadyExists("test-msg"));

        assertThat(participantContextService.updateParticipant(context.getParticipantContextId(), IdentityHubParticipantContext::deactivate)).isFailed()
                .detail().isEqualTo("test-msg");

        verify(participantContextStore).findByIdForUpdate(anyString());
        verify(participantContextStore).update(any());
        verifyNoMoreInteractions(participantContextStore, observableMock);
    }

    @Test
    void query() {
        var ctx = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(
                createContext(),
                createContext(),
                createContext())));

        assertThat(participantContextService.query(QuerySpec.max()))
                .isSucceeded()
                .satisfies(res -> assertThat(res).hasSize(3));

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(vault);
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .key(createKey().build())
                .active(true)
                .did("did:web:test-id")
                .participantContextId("test-id");
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance().keyId("test-kie")
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .privateKeyAlias("private-alias")
                .publicKeyJwk(createJwk());
    }

    private IdentityHubParticipantContext createContext() {
        return IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .did("did:web:test-id")
                .state(ParticipantContextState.CREATED)
                .apiTokenAlias("test-alias")
                .build();
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
}
