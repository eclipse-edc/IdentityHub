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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ParticipantContextServiceImplTest {

    private final Vault vault = mock();
    private final ParticipantContextStore participantContextStore = mock();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ParticipantContextServiceImpl participantContextService = new ParticipantContextServiceImpl(participantContextStore, vault, new NoopTransactionContext(), new Base64StringGenerator());

    @Test
    void createParticipantContext() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());

        var ctx = createContext();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @Test
    void createParticipantContext_storageFails() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.success());

        var ctx = createContext();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isSucceeded();

        verify(participantContextStore).create(any());
        verifyNoMoreInteractions(vault, participantContextStore);
    }

    @Test
    void createParticipantContext_whenExists() {
        when(participantContextStore.create(any())).thenReturn(StoreResult.alreadyExists("test-failure"));

        var ctx = createContext();
        assertThat(participantContextService.createParticipantContext(ctx))
                .isFailed()
                .satisfies(f -> Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(participantContextStore).create(any());
        verifyNoMoreInteractions(vault, participantContextStore);

    }

    @Test
    void getParticipantContext() {
        var ctx = createContext();
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(Stream.of(ctx)));

        assertThat(participantContextService.getParticipantContext("test-id"))
                .isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(ctx);

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void getParticipantContext_whenNotExists() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(Stream.of()));
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
        when(participantContextStore.deleteById(anyString())).thenReturn(StoreResult.success());
        assertThat(participantContextService.deleteParticipantContext("test-id")).isSucceeded();

        verify(participantContextStore).deleteById(anyString());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void deleteParticipantContext_whenNotExists() {
        when(participantContextStore.deleteById(any())).thenReturn(StoreResult.notFound("foo bar"));
        assertThat(participantContextService.deleteParticipantContext("test-id"))
                .isFailed()
                .satisfies(f -> {
                    Assertions.assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
                    Assertions.assertThat(f.getFailureDetail()).isEqualTo("foo bar");
                });

        verify(participantContextStore).deleteById(anyString());
        verifyNoMoreInteractions(vault);
    }

    @Test
    void regenerateApiToken() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(Stream.of(createContext())));
        when(vault.storeSecret(eq("test-alias"), anyString())).thenReturn(Result.success());

        assertThat(participantContextService.regenerateApiToken("test-id")).isSucceeded().isNotNull();

        verify(participantContextStore).query(any());
        verify(vault).storeSecret(eq("test-alias"), argThat(s -> s.length() >= 64));
    }

    @Test
    void regenerateApiToken_vaultFails() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(Stream.of(createContext())));
        when(vault.storeSecret(eq("test-alias"), anyString())).thenReturn(Result.failure("test failure"));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("Could not store new API token: test failure.");

        verify(participantContextStore).query(any());
        verify(vault).storeSecret(eq("test-alias"), anyString());
    }

    @Test
    void regenerateApiToken_whenNotFound() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(Stream.of()));

        assertThat(participantContextService.regenerateApiToken("test-id")).isFailed().detail().isEqualTo("No ParticipantContext with ID 'test-id' was found.");

        verify(participantContextStore).query(any());
        verifyNoMoreInteractions(participantContextStore, vault);
    }

    private ParticipantContext createContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId("test-id")
                .state(ParticipantContextState.CREATED)
                .apiTokenAlias("test-alias")
                .build();
    }
}