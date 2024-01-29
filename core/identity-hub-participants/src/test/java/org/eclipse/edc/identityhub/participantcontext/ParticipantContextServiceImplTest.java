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
import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.core.security.KeyParserRegistryImpl;
import org.eclipse.edc.connector.core.security.keyparsers.PemParser;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ParticipantContextServiceImplTest {

    private final Vault vault = mock();
    private final ParticipantContextStore participantContextStore = mock();
    private ParticipantContextServiceImpl participantContextService;
    private DidDocumentService didDocumentService;
    private final ParticipantContextObservable observableMock = mock();

    @BeforeEach
    void setUp() {
        didDocumentService = mock();
        when(didDocumentService.store(any(), anyString())).thenReturn(success());
        when(didDocumentService.publish(anyString())).thenReturn(success());
        var keyParserRegistry = new KeyParserRegistryImpl();
        keyParserRegistry.register(new PemParser(mock()));
        participantContextService = new ParticipantContextServiceImpl(participantContextStore, vault, new NoopTransactionContext(), keyParserRegistry, didDocumentService, observableMock);
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withPublicKeyPem(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString())).thenReturn(Result.success());

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
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verify(didDocumentService).store(argThat(dd -> dd.getId().equals(ctx.getDid())), anyString());
        verify(didDocumentService, times(isActive ? 1 : 0)).publish(anyString());
        verify(vault).storeSecret(eq(ctx.getParticipantId() + "-apikey"), anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withPublicKeyJwk(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString())).thenReturn(Result.success());

        var ctx = createManifest().active(isActive)
                .build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verify(didDocumentService).store(argThat(dd -> dd.getId().equals(ctx.getDid())), anyString());
        verify(didDocumentService, times(isActive ? 1 : 0)).publish(anyString());
        verify(vault).storeSecret(eq(ctx.getParticipantId() + "-apikey"), anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @ParameterizedTest(name = "isActive: {0}")
    @ValueSource(booleans = {true, false})
    void createParticipantContext_withKeyGenParams(boolean isActive) {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString())).thenReturn(Result.success());
        var ctx = createManifest()
                .active(isActive)
                .key(createKey().publicKeyPem(null).publicKeyJwk(null)
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "ed25519"))
                        .build())
                .build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verify(vault).storeSecret(eq(ctx.getKey().getPrivateKeyAlias()), anyString());
        verify(vault).storeSecret(eq(ctx.getParticipantId() + "-apikey"), anyString());

        verify(didDocumentService).store(argThat(dd -> dd.getId().equals(ctx.getDid())), anyString());
        verify(didDocumentService, times(isActive ? 1 : 0)).publish(anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @Test
    void createParticipantContext_storageFails() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());
        when(vault.storeSecret(anyString(), anyString())).thenReturn(Result.success());

        var ctx = createManifest().build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verify(vault).storeSecret(eq(ctx.getParticipantId() + "-apikey"), anyString());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @Test
    void createParticipantContext_whenExists() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.alreadyExists("test-failure"));

        var ctx = createManifest().build();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed()
                .satisfies(f -> Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(participantContextStore).create(any());
        verifyNoMoreInteractions(vault, participantContextStore);

    }

    @Test
    void getParticipantContext() {
        var ctx = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(ctx)));

        assertThat(participantContextService.getParticipantContext("test-id"))
                .isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(ctx);

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void getParticipantContext_whenNotExists() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of()));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("No ParticipantContext with ID 'test-id' was found.");
                });

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void getParticipantContext_whenStorageFails() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.notFound("foo bar"));
        assertThat(participantContextService.getParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void deleteParticipantContext() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(didDocumentService.unpublish(any())).thenReturn(success());
        when(didDocumentService.deleteById(any())).thenReturn(success());
        assertThat(participantContextService.deleteParticipantContext("test-id")).isSucceeded();

        verify(participantContextStore).deleteById(anyString());
        verify(didDocumentService).unpublish(any());
        verify(didDocumentService).deleteById(any());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void deleteParticipantContext_deleteDidFails() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(didDocumentService.unpublish(any())).thenReturn(success());
        when(didDocumentService.deleteById(any())).thenReturn(badRequest("test-message"));
        assertThat(participantContextService.deleteParticipantContext("test-id")).isFailed()
                .detail().isEqualTo("test-message");

        verify(participantContextStore).deleteById(anyString());
        verify(participantContextStore).query(any());
        verify(didDocumentService).unpublish(any());
        verify(didDocumentService).deleteById(any());
        verifyNoMoreInteractions(vault, didDocumentService, participantContextStore);
    }

    @Test
    void deleteParticipantContext_unpublishDidFails() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(didDocumentService.unpublish(any())).thenReturn(badRequest("test-message"));
        assertThat(participantContextService.deleteParticipantContext("test-id")).isFailed()
                .detail().isEqualTo("test-message");

        verify(participantContextStore).deleteById(anyString());
        verify(participantContextStore).query(any());
        verify(didDocumentService).unpublish(any());
        verifyNoMoreInteractions(vault, didDocumentService, participantContextStore);
    }

    @Test
    void deleteParticipantContext_whenNotExists() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(participantContextStore.deleteById(any())).thenReturn(StoreResult.notFound("foo bar"));
        when(didDocumentService.unpublish(any())).thenReturn(success());
        when(didDocumentService.deleteById(any())).thenReturn(success());
        assertThat(participantContextService.deleteParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).deleteById(anyString());
        verifyNoMoreInteractions(vault, didDocumentService);
    }

    @Test
    void regenerateApiToken() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(vault.storeSecret(eq("test-alias"), anyString())).thenReturn(Result.success());

        assertThat(participantContextService.regenerateApiToken("test-id")).isSucceeded().isNotNull();

        verify(participantContextStore).query(any());
        verify(vault).storeSecret(eq("test-alias"), argThat(s -> s.length() >= 64));
    }

    @Test
    void regenerateApiToken_vaultFails() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(createContext())));
        when(vault.storeSecret(eq("test-alias"), anyString())).thenReturn(Result.failure("test failure"));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("Could not store new API token: test failure.");

        verify(participantContextStore).query(any());
        verify(vault).storeSecret(eq("test-alias"), anyString());
    }

    @Test
    void regenerateApiToken_whenNotFound() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of()));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("No ParticipantContext with ID 'test-id' was found.");

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(participantContextStore, vault);
    }

    @Test
    void update() {
        var context = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(context)));
        when(participantContextStore.update(any())).thenReturn(StoreResult.success());
        assertThat(participantContextService.updateParticipant(context.getParticipantId(), ParticipantContext::deactivate)).isSucceeded();

        verify(participantContextStore).query(any());
        verify(participantContextStore).update(any());

    }

    @Test
    void update_whenNotFound() {
        var context = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.notFound("foobar"));
        assertThat(participantContextService.updateParticipant(context.getParticipantId(), ParticipantContext::deactivate)).isFailed()
                .detail().isEqualTo("ParticipantContext with ID 'test-id' not found.");

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(participantContextStore);
    }

    @Test
    void update_whenStoreUpdateFails() {
        var context = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(context)));
        when(participantContextStore.update(any())).thenReturn(StoreResult.alreadyExists("test-msg"));

        assertThat(participantContextService.updateParticipant(context.getParticipantId(), ParticipantContext::deactivate)).isFailed()
                .detail().isEqualTo("test-msg");

        verify(participantContextStore).query(any());
        verify(participantContextStore).update(any());
        verifyNoMoreInteractions(participantContextStore);
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .key(createKey().build())
                .active(true)
                .did("did:web:test-id")
                .participantId("test-id");
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance().keyId("test-kie")
                .privateKeyAlias("private-alias")
                .publicKeyJwk(createJwk());
    }

    private ParticipantContext createContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId("test-id")
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