/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.participant;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HolderDataCleanupTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private final CredentialStore credentialStore = mock();
    private final HolderCredentialRequestStore credentialRequestStore = mock();
    private final CredentialOfferStore credentialOfferStore = mock();
    private final Monitor monitor = mock();
    private final HolderDataCleanup cleanup = new HolderDataCleanup(credentialStore, credentialRequestStore, credentialOfferStore,
            new NoopTransactionContext(), monitor);

    @Test
    void on_participantContextDeleted_shouldDeleteHolderData() {
        var credential = mock(VerifiableCredentialResource.class);
        when(credential.getId()).thenReturn("credential-id");
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credential)));
        when(credentialStore.deleteById(anyString(), anyString())).thenReturn(StoreResult.success());
        when(credentialRequestStore.query(any())).thenReturn(List.of(request("request-id")));
        when(credentialRequestStore.deleteById(anyString())).thenReturn(StoreResult.success());
        when(credentialOfferStore.query(any())).thenReturn(List.of(offer("offer-id")));
        when(credentialOfferStore.deleteById(anyString())).thenReturn(StoreResult.success());

        cleanup.on(deletedEvent());

        verify(credentialStore).deleteById(PARTICIPANT_CONTEXT_ID, "credential-id");
        verify(credentialRequestStore).deleteById("request-id");
        verify(credentialOfferStore).deleteById("offer-id");
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void on_participantContextDeleted_shouldOnlyQueryCredentialsHeldByParticipant() {
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of()));

        cleanup.on(deletedEvent());

        var captor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(credentialStore).query(captor.capture());
        assertThat(captor.getValue().getFilterExpression()).containsExactlyInAnyOrder(
                new Criterion("participantContextId", "=", PARTICIPANT_CONTEXT_ID),
                new Criterion("usage", "=", CredentialUsage.Holder.toString()));
    }

    @Test
    void on_deletionFails_shouldWarnAndContinue() {
        when(credentialStore.query(any())).thenReturn(StoreResult.generalError("query failed"));
        when(credentialRequestStore.query(any())).thenReturn(List.of(request("request-id")));
        when(credentialRequestStore.deleteById(anyString())).thenReturn(StoreResult.alreadyLeased("leased"));
        when(credentialOfferStore.query(any())).thenReturn(List.of(offer("offer-id")));
        when(credentialOfferStore.deleteById(anyString())).thenReturn(StoreResult.success());

        cleanup.on(deletedEvent());

        verify(monitor).warning(contains("query failed"));
        verify(monitor).warning(contains("leased"));
        verify(credentialOfferStore).deleteById("offer-id");
    }

    private EventEnvelope<ParticipantContextDeleted> deletedEvent() {
        var event = ParticipantContextDeleted.Builder.newInstance().participantContextId(PARTICIPANT_CONTEXT_ID).build();
        return EventEnvelope.Builder.newInstance().payload(event).at(System.currentTimeMillis()).build();
    }

    private HolderCredentialRequest request(String id) {
        return HolderCredentialRequest.Builder.newInstance()
                .id(id)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .issuerDid("did:web:issuer")
                .requestedCredential("credential-object-id", "TestCredential", "VC1_0_JWT")
                .build();
    }

    private CredentialOffer offer(String id) {
        return CredentialOffer.Builder.newInstance()
                .id(id)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .issuer("did:web:issuer")
                .state(CredentialOfferStatus.RECEIVED.code())
                .build();
    }
}
