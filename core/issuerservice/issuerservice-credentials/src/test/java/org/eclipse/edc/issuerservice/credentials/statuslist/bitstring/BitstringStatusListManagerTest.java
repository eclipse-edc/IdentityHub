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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.CURRENT_INDEX;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.DEFAULT_BITSTRING_SIZE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.IS_ACTIVE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.PUBLIC_URL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BitstringStatusListManagerTest {
    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String CREDENTIAL_URL = "http://foo.bar.com/barbaz/credential1.json";
    private final CredentialStore store = mock();
    private final CredentialGeneratorRegistry generator = mock();
    private final ParticipantContextService participantContextService = mock();
    private final StatusListCredentialPublisher publisher = mock();
    private final BitstringStatusListManager manager = new BitstringStatusListManager(store, new NoopTransactionContext(), generator, participantContextService, publisher);

    @BeforeEach
    void setUp() {
        when(publisher.publish(any())).thenReturn(Result.success(CREDENTIAL_URL));
        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID)))
                .thenReturn(success(ParticipantContext.Builder.newInstance()
                        .participantContextId(PARTICIPANT_CONTEXT_ID)
                        .apiTokenAlias("api-token-alias")
                        .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                        .build()));

        when(generator.signCredential(any(), any(), any()))
                .thenAnswer(i -> Result.success(new VerifiableCredentialContainer("test-raw-token", CredentialFormat.VC1_0_JWT, i.getArgument(1))));
    }

    @Test
    void getActiveCredential() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, true)
                .build())));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(42);
                });

        verify(store).query(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_credentialNotFound() {
        when(store.query(any())).thenReturn(StoreResult.notFound("foobar"));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed().detail().contains("foobar");
        verify(store).query(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenEmptyResult_shouldCreateNew() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                    assertThat(e.credentialUrl()).isEqualTo(CREDENTIAL_URL);
                });

        verify(store).query(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenFull_shouldCreateNew() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, DEFAULT_BITSTRING_SIZE) // credential is saturated
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, true)
                .build())));
        when(store.create(any())).thenReturn(StoreResult.success());
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                });

        verify(store).query(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenNotActive_shouldCreateNew() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, false) // triggers creation
                .build())));
        when(store.create(any())).thenReturn(StoreResult.success());
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                });

        verify(store).query(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenCreateNew_signingFails() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
        when(generator.signCredential(anyString(), any(), any()))
                .thenReturn(Result.failure("signing failure"));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed()
                .detail().contains("signing failure");

        verify(store).query(any());
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenCreateNew_participantNotFound() {
        when(participantContextService.getParticipantContext(PARTICIPANT_CONTEXT_ID))
                .thenReturn(ServiceResult.notFound("foobar"));
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed()
                .detail().contains("foobar");

        verify(store).query(any());
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex() {
        var entry = new BitstringStatusListCredentialEntry(42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        when(store.update(any())).thenReturn(StoreResult.success());
        assertThat(manager.incrementIndex(entry)).isSucceeded();

        verify(store).update(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenNotExist_shouldInsert() {
        when(store.update(any())).thenReturn(StoreResult.notFound("foo"));
        var entry = new BitstringStatusListCredentialEntry(42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        when(store.create(any())).thenReturn(StoreResult.success());
        assertThat(manager.incrementIndex(entry)).isSucceeded();

        verify(store).update(any());
        verify(store).create(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenUpdateFails() {
        when(store.update(any())).thenReturn(StoreResult.generalError("foo"));
        var entry = new BitstringStatusListCredentialEntry(42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        assertThat(manager.incrementIndex(entry)).isFailed().detail().contains("foo");

        verify(store).update(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenStoreFails_shouldReturnFailure() {
        when(store.update(any())).thenReturn(StoreResult.notFound("foo"));
        when(store.create(any())).thenReturn(StoreResult.generalError("bar"));
        var entry = new BitstringStatusListCredentialEntry(42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        assertThat(manager.incrementIndex(entry)).isFailed().detail().contains("bar");

        verify(store).update(any());
        verify(store).create(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    private VerifiableCredentialResource hasParticipantId(String participantContextId) {
        return argThat(res -> res.getParticipantContextId().equals(participantContextId));
    }

    private VerifiableCredentialResource.Builder createVerifiableCredentialResource() {
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId("issuer-id")
                .holderId("holder-id");
    }

}
