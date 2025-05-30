/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.credential.offer.handler;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferEvent;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CredentialOfferHandlerTest {

    private final Monitor monitor = mock();
    private final CredentialRequestManager requestManager = mock();
    private final DcpProfileRegistry profileRegistry = mock();

    private final CredentialOfferStore credentialOfferStore = mock();
    private final CredentialOfferHandler credentialOfferHandler = new CredentialOfferHandler(monitor, requestManager, profileRegistry, credentialOfferStore, new NoopTransactionContext());

    @BeforeEach
    void setUp() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer());
        when(requestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenReturn(ServiceResult.success(UUID.randomUUID().toString()));
        when(profileRegistry.getProfile(startsWith("test-profile")))
                .thenReturn(new DcpProfile("test-profile", CredentialFormat.VC1_0_JWT, "BitStringStatusList"));
    }

    @Test
    void onCredentialOfferEvent() {


        credentialOfferHandler.on(event());
        verifyNoInteractions(monitor);
        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_initiateFails() {
        when(requestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenReturn(ServiceResult.badRequest("foobar"));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
        verify(monitor).warning(matches("Could not initiate credential request.*foobar"));
    }


    @Test
    void onUnknownEvent_shouldLogWarning() {
        credentialOfferHandler.on(EventEnvelope.Builder.newInstance().at(1).payload(new Event() {
            @Override
            public String name() {
                return "foobar";
            }
        }).build());
        verify(monitor).warning(anyString());
        verifyNoInteractions(requestManager);
        verifyNoMoreInteractions(profileRegistry);
    }

    @Test
    void onCredentialOfferEvent_noValidCredentialType() {
        when(profileRegistry.getProfile(startsWith("test-profile")))
                .thenReturn(null);

        credentialOfferHandler.on(event());
        verifyNoInteractions(requestManager);
        verify(monitor).warning(contains("no credential format could be derived"));
        verify(monitor).warning(contains("Could not process credential offer"));
    }

    @Test
    void onCredentialOfferEvent_storeUpdateFails() {
        doThrow(new EdcPersistenceException("foo")).when(credentialOfferStore).save(any());
        credentialOfferHandler.on(event());

        verify(monitor).warning(contains("Could not persist CredentialOffer in database: foo"));
    }

    private CredentialOffer createOffer() {
        return CredentialOffer.Builder.newInstance()
                .state(CredentialOfferStatus.RECEIVED.code())
                .participantContextId("test-participant")
                .issuer("did:web:issuer")
                .id(UUID.randomUUID().toString())
                .credentialObject(CredentialObject.Builder.newInstance()
                        .profile("test-profile1")
                        .profile("test-profile2")
                        .build())
                .build();
    }


    private EventEnvelope<CredentialOfferEvent> event() {
        var payload = CredentialOfferReceived.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId("test-participant")
                .issuer("did:web:issuer")
                .build();
        return EventEnvelope.Builder.newInstance()
                .at(Instant.now().toEpochMilli())
                .payload(payload)
                .build();
    }
}