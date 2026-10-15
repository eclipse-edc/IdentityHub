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
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferEvent;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private final HolderCredentialRequestStore holderCredentialRequestStore = mock();
    private final CredentialStore credentialStore = mock();
    private final CredentialOfferHandler credentialOfferHandler = createHandler(Set.of("did:web:issuer"));

    @BeforeEach
    void setUp() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer());
        when(credentialOfferStore.save(any())).thenReturn(StoreResult.success());
        when(requestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenReturn(ServiceResult.success(UUID.randomUUID().toString()));
        when(profileRegistry.getProfile(startsWith("test-profile")))
                .thenReturn(new DcpProfile("test-profile", CredentialFormat.VC1_0_JWT, "BitStringStatusList"));
        when(profileRegistry.getProfile("jose-profile"))
                .thenReturn(new DcpProfile("jose-profile", CredentialFormat.VC2_0_JOSE, "BitStringStatusList"));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of());
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of()));
    }

    @Test
    void onCredentialOfferEvent_untrustedIssuer_shouldNotInitiateRequest() {
        var handler = createHandler(Set.of("did:web:another-issuer"));

        handler.on(event());

        verify(requestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
        // the offer stays RECEIVED, so that it can be acted upon manually
        verify(credentialOfferStore, never()).save(any());
        verify(monitor).info(contains("not trusted"));
    }

    @Test
    void onCredentialOfferEvent_noTrustedIssuersConfigured_shouldInitiateRequest() {
        var handler = createHandler(Set.of());

        handler.on(event());

        verifyNoInteractions(credentialStore);
        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_issuerOfHeldCredential_shouldInitiateRequest() {
        var handler = createHandler(Set.of("did:web:another-issuer"));
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(mock(VerifiableCredentialResource.class))));

        handler.on(event());

        verify(credentialStore).query(argThat((QuerySpec query) -> query.getFilterExpression().containsAll(List.of(
                criterion("participantContextId", "=", "test-participant"),
                criterion("usage", "=", CredentialUsage.Holder.toString()),
                criterion("issuerId", "=", "did:web:issuer")))));
        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_trustLookupFails_shouldNotInitiateRequest() {
        var handler = createHandler(Set.of("did:web:another-issuer"));
        when(credentialStore.query(any())).thenReturn(StoreResult.generalError("query failed"));

        handler.on(event());

        verify(requestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
        verify(credentialOfferStore, never()).save(any());
        verify(monitor).warning(contains("query failed"));
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
        verify(credentialOfferStore).save(argThat(offer -> offer.getState() == CredentialOfferStatus.REJECTED.code()));
    }

    @Test
    void onCredentialOfferEvent_storeUpdateFails() {
        doThrow(new EdcPersistenceException("foo")).when(credentialOfferStore).save(any());
        credentialOfferHandler.on(event());

        verify(monitor).warning(contains("Could not persist CredentialOffer in database: foo"));
    }

    // A5.3: when initiateRequest fails, the offer must not be transitioned to PROCESSED (currently it is)
    @Test
    @DisplayName("A5.3: an offer is not transitioned to PROCESSED when the credential request cannot be initiated")
    void onCredentialOfferEvent_initiateFails_shouldNotTransitionToProcessed() {
        when(requestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenReturn(ServiceResult.badRequest("foobar"));

        credentialOfferHandler.on(event());

        // the offer must not be persisted as PROCESSED when the request could not be initiated (it should stay RECEIVED or become REJECTED)
        verify(credentialOfferStore, never()).save(argThat(offer -> offer.getState() == CredentialOfferStatus.PROCESSED.code()));
    }

    // CS-OFF-06: the same CredentialOfferReceived event / identical offer handled twice -> second handling is a no-op: no second initiateRequest, no duplicate HolderCredentialRequest
    @Test
    @DisplayName("CS-OFF-06: handling the identical credential offer twice initiates only one credential request")
    void onCredentialOfferEvent_sameOfferTwice_shouldBeIdempotent() {
        var event = event();

        // handle the identical event twice
        credentialOfferHandler.on(event);
        credentialOfferHandler.on(event);

        // only one credential request is initiated, no duplicate HolderCredentialRequest is spawned
        verify(requestManager, times(1)).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_openRequestWithSameTypesAndFormats_shouldSkip() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(
                credentialObject("MembershipCredential", "test-profile"),
                credentialObject("DemoCredential", "jose-profile")));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("existing-holder-pid",
                new RequestedCredential("other-id-1", "DemoCredential", "VC2_0_JOSE"),
                new RequestedCredential("other-id-2", "MembershipCredential", "VC1_0_JWT"))));

        credentialOfferHandler.on(event());

        verify(requestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
        verify(monitor).info(contains("existing-holder-pid"));
        verify(credentialOfferStore).save(argThat(offer -> offer.getState() == CredentialOfferStatus.PROCESSED.code()));
    }

    @Test
    void onCredentialOfferEvent_shouldQueryOpenRequestsOfSameParticipantAndIssuer() {
        credentialOfferHandler.on(event());

        verify(holderCredentialRequestStore).query(argThat((QuerySpec query) -> query.getLimit() == Integer.MAX_VALUE && query.getFilterExpression().containsAll(List.of(
                criterion("participantContextId", "=", "test-participant"),
                criterion("issuerDid", "=", "did:web:issuer"),
                criterion("state", "in", List.of(HolderRequestState.CREATED.code(), HolderRequestState.REQUESTING.code(), HolderRequestState.REQUESTED.code()))))));
    }

    @Test
    void onCredentialOfferEvent_openRequestWithDifferentFormat_shouldInitiateRequest() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(
                credentialObject("MembershipCredential", "test-profile"),
                credentialObject("DemoCredential", "jose-profile")));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("existing-holder-pid",
                new RequestedCredential("other-id-1", "MembershipCredential", "VC1_0_JWT"),
                new RequestedCredential("other-id-2", "DemoCredential", "VC1_0_JWT"))));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_openRequestWithDifferentType_shouldInitiateRequest() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(
                credentialObject("MembershipCredential", "test-profile"),
                credentialObject("DemoCredential", "jose-profile")));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("existing-holder-pid",
                new RequestedCredential("other-id-1", "MembershipCredential", "VC1_0_JWT"),
                new RequestedCredential("other-id-2", "OtherCredential", "VC2_0_JOSE"))));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_openRequestWithMoreCredentials_shouldInitiateRequest() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(credentialObject("MembershipCredential", "test-profile")));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("existing-holder-pid",
                new RequestedCredential("other-id-1", "MembershipCredential", "VC1_0_JWT"),
                new RequestedCredential("other-id-2", "DemoCredential", "VC2_0_JOSE"))));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_openRequestWithFewerCredentials_shouldInitiateRequest() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(
                credentialObject("MembershipCredential", "test-profile"),
                credentialObject("DemoCredential", "jose-profile")));
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("existing-holder-pid",
                new RequestedCredential("other-id-1", "MembershipCredential", "VC1_0_JWT"))));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), anyList());
    }

    @Test
    void onCredentialOfferEvent_noOpenRequest_shouldInitiateRequest() {
        when(credentialOfferStore.findById(anyString())).thenReturn(createOffer(credentialObject("MembershipCredential", "test-profile")));

        credentialOfferHandler.on(event());

        verify(requestManager).initiateRequest(eq("test-participant"), eq("did:web:issuer"), anyString(), argThat(requested ->
                requested.size() == 1 && requested.get(0).credentialType().equals("MembershipCredential") && requested.get(0).format().equals("VC1_0_JWT")));
        verify(credentialOfferStore).save(argThat(offer -> offer.getState() == CredentialOfferStatus.PROCESSED.code()));
    }

    @Test
    void onCredentialOfferEvent_resentOffer_shouldSkipWhileFirstRequestIsOpen() {
        var requested = new ArrayList<List<RequestedCredential>>();
        when(requestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenAnswer(invocation -> {
            requested.add(invocation.getArgument(3));
            return ServiceResult.success(invocation.getArgument(2));
        });
        when(credentialOfferStore.findById(anyString()))
                .thenReturn(createOffer(credentialObject("MembershipCredential", "test-profile")))
                .thenReturn(createOffer(credentialObject("MembershipCredential", "test-profile")));

        credentialOfferHandler.on(event());
        assertThat(requested).hasSize(1);
        when(holderCredentialRequestStore.query(any())).thenReturn(List.of(openRequest("first-holder-pid", requested.get(0).toArray(RequestedCredential[]::new))));
        clearInvocations(requestManager);

        credentialOfferHandler.on(event());

        verify(requestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
    }

    private CredentialOfferHandler createHandler(Set<String> trustedIssuers) {
        return new CredentialOfferHandler(monitor, requestManager, profileRegistry, credentialOfferStore, holderCredentialRequestStore, new NoopTransactionContext(),
                credentialStore, trustedIssuers);
    }

    private CredentialOffer createOffer(CredentialObject... credentialObjects) {
        return CredentialOffer.Builder.newInstance()
                .state(CredentialOfferStatus.RECEIVED.code())
                .participantContextId("test-participant")
                .issuer("did:web:issuer")
                .id(UUID.randomUUID().toString())
                .credentialObjects(new ArrayList<>(List.of(credentialObjects)))
                .build();
    }

    private CredentialObject credentialObject(String type, String profile) {
        return CredentialObject.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .credentialType(type)
                .profile(profile)
                .build();
    }

    private HolderCredentialRequest openRequest(String holderPid, RequestedCredential... requestedCredentials) {
        var builder = HolderCredentialRequest.Builder.newInstance()
                .id(holderPid)
                .participantContextId("test-participant")
                .issuerDid("did:web:issuer")
                .state(HolderRequestState.REQUESTED.code());
        for (var rc : requestedCredentials) {
            builder.requestedCredential(rc.id(), rc.credentialType(), rc.format());
        }
        return builder.build();
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