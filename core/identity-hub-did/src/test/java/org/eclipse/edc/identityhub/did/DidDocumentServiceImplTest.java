/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identithub.spi.did.DidDocumentPublisher;
import org.eclipse.edc.identithub.spi.did.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.spi.did.model.DidResource;
import org.eclipse.edc.identithub.spi.did.model.DidState;
import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.iam.did.spi.document.DidConstants.JSON_WEB_KEY_2020;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DidDocumentServiceImplTest {
    public static final String TEST_DID = "did:web:testdid";
    private static final String TEST_PARTICIPANT_ID = "test-participant";
    private final DidResourceStore didResourceStoreMock = mock();
    private final DidDocumentPublisherRegistry publisherRegistry = mock();
    private final DidDocumentPublisher publisherMock = mock();
    private final ParticipantContextService participantContextServiceMock = mock();
    private DidDocumentServiceImpl service;
    private Monitor monitorMock;

    @BeforeEach
    void setUp() {
        var trx = new NoopTransactionContext();
        when(publisherRegistry.getPublisher(startsWith("did:web:"))).thenReturn(publisherMock);

        var registry = new KeyParserRegistryImpl();
        registry.register(new JwkParser(new ObjectMapper(), mock()));
        registry.register(new PemParser(mock()));
        monitorMock = mock();
        service = new DidDocumentServiceImpl(trx, didResourceStoreMock, publisherRegistry, participantContextServiceMock, monitorMock, registry);

        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.ACTIVATED)
                .build()));
    }

    @Test
    void store() {
        var doc = createDidDocument().build();
        when(didResourceStoreMock.save(argThat(dr -> dr.getDocument().equals(doc)))).thenReturn(StoreResult.success());
        assertThat(service.store(doc, "test-participant")).isSucceeded();
        verify(didResourceStoreMock).save(argThat(didResource -> didResource.getState() == DidState.GENERATED.code()));
    }

    @Test
    void store_alreadyExists() {
        var doc = createDidDocument().build();
        when(didResourceStoreMock.save(argThat(dr -> dr.getDocument().equals(doc)))).thenReturn(StoreResult.alreadyExists("foo"));
        assertThat(service.store(doc, "test-participant")).isFailed().detail().isEqualTo("foo");
        verify(didResourceStoreMock).save(any());
        verifyNoInteractions(publisherMock);
    }

    @Test
    void deleteById() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.UNPUBLISHED).document(doc).build());
        when(didResourceStoreMock.deleteById(any())).thenReturn(StoreResult.success());

        assertThat(service.deleteById(did)).isSucceeded();

        verify(didResourceStoreMock).findById(did);
        verify(didResourceStoreMock).deleteById(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void deleteById_alreadyPublished() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());

        assertThat(service.deleteById(did)).isFailed()
                .detail()
                .isEqualTo("Cannot delete DID '%s' because it is already published. Un-publish first!".formatted(did));

        verify(didResourceStoreMock).findById(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void deleteById_notExists() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.UNPUBLISHED).document(doc).build());
        when(didResourceStoreMock.deleteById(any())).thenReturn(StoreResult.notFound("test-message"));

        assertThat(service.deleteById(did)).isFailed().detail().isEqualTo("test-message");

        verify(didResourceStoreMock).findById(did);
        verify(didResourceStoreMock).deleteById(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void publish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(publisherMock.publish(did)).thenReturn(Result.success());

        assertThat(service.publish(did)).isSucceeded();

        verify(didResourceStoreMock).findById(did);
        verify(publisherMock).publish(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void publish_notExist() {
        var did = "did:web:test-did";
        when(didResourceStoreMock.findById(eq(did))).thenReturn(null);

        assertThat(service.publish(did)).isFailed()
                .detail().isEqualTo(service.notFoundMessage(did));

        verify(didResourceStoreMock).findById(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void publish_noPublisherFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(publisherRegistry.getPublisher(any())).thenReturn(null);
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        assertThat(service.publish(did)).isFailed().detail()
                .isEqualTo(service.noPublisherFoundMessage(did));

        verify(didResourceStoreMock).findById(did);
        verify(publisherRegistry).getPublisher(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void publish_publisherReportsError() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(publisherMock.publish(did)).thenReturn(Result.failure("test-failure"));

        assertThat(service.publish(did)).isFailed()
                .detail()
                .isEqualTo("test-failure");

        verify(didResourceStoreMock).findById(did);
        verify(publisherMock).publish(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void unpublish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());
        when(publisherMock.unpublish(did)).thenReturn(Result.success());
        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        assertThat(service.unpublish(did)).isSucceeded();

        verify(didResourceStoreMock).findById(did);
        verify(publisherMock).unpublish(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void unpublish_notExist() {
        var did = "did:web:test-did";
        when(didResourceStoreMock.findById(eq(did))).thenReturn(null);

        assertThat(service.unpublish(did)).isFailed()
                .detail().isEqualTo(service.notFoundMessage(did));

        verify(didResourceStoreMock).findById(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void unpublish_noPublisherFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(publisherRegistry.getPublisher(any())).thenReturn(null);
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());
        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        assertThat(service.unpublish(did)).isFailed().detail()
                .isEqualTo(service.noPublisherFoundMessage(did));

        verify(didResourceStoreMock).findById(did);
        verify(publisherRegistry).getPublisher(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void unpublish_publisherReportsError() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());
        when(publisherMock.unpublish(did)).thenReturn(Result.failure("test-failure"));
        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        assertThat(service.unpublish(did)).isFailed()
                .detail()
                .isEqualTo("test-failure");

        verify(didResourceStoreMock).findById(did);
        verify(publisherMock).unpublish(did);
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock);
    }

    @Test
    void queryDocuments() {
        var q = QuerySpec.max();
        var doc = createDidDocument().build();
        var res = DidResource.Builder.newInstance().did(doc.getId()).state(DidState.PUBLISHED).document(doc).build();
        when(didResourceStoreMock.query(any())).thenReturn(List.of(res));

        assertThat(service.queryDocuments(q)).isSucceeded();

        verify(didResourceStoreMock).query(eq(q));
        verifyNoMoreInteractions(publisherMock, didResourceStoreMock, publisherRegistry);
    }

    @Test
    void addEndpoint() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(didResourceStoreMock.update(any())).thenReturn(StoreResult.success());
        var res = service.addService(did, new Service("new-id", "test-type", "https://test.com"));
        assertThat(res).isSucceeded();

        verify(didResourceStoreMock).findById(eq(did));
        verify(didResourceStoreMock).update(any());
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void addEndpoint_alreadyExists() {
        var newService = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(newService)).build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        var res = service.addService(did, newService);
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' already contains a service endpoint with ID 'new-id'.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void addEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(null);
        var res = service.addService(did, new Service("test-id", "test-type", "https://test.com"));
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void replaceEndpoint() {
        var toReplace = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(toReplace)).build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(didResourceStoreMock.update(any())).thenReturn(StoreResult.success());

        var res = service.replaceService(did, toReplace);
        assertThat(res).isSucceeded();

        verify(didResourceStoreMock).findById(eq(did));
        verify(didResourceStoreMock).update(any());
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void replaceEndpoint_doesNotExist() {
        var replace = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        var res = service.replaceService(did, replace);
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' does not contain a service endpoint with ID 'new-id'.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void replaceEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(null);
        var res = service.replaceService(did, new Service("test-id", "test-type", "https://test.com"));
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void removeEndpoint() {
        var toRemove = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(toRemove)).build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(didResourceStoreMock.update(any())).thenReturn(StoreResult.success());

        var res = service.removeService(did, toRemove.getId());
        assertThat(res).isSucceeded();

        verify(didResourceStoreMock).findById(eq(did));
        verify(didResourceStoreMock).update(any());
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void removeEndpoint_doesNotExist() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        var res = service.removeService(did, "not-exist-id");
        assertThat(res).isFailed()
                .detail().isEqualTo("DID 'did:web:testdid' does not contain a service endpoint with ID 'not-exist-id'.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @Test
    void removeEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(null);
        var res = service.removeService(did, "does-not-matter-id");
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(didResourceStoreMock).findById(eq(did));
        verifyNoMoreInteractions(didResourceStoreMock, publisherMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    void onParticipantContextUpdated_whenDeactivates_shouldUnpublish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        var participantId = "test-id";
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(didResource);
        when(didResourceStoreMock.query(any())).thenReturn(List.of(didResource));
        when(publisherMock.unpublish(anyString())).thenReturn(Result.success());

        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        service.on(EventEnvelope.Builder.newInstance()
                .payload(ParticipantContextUpdated.Builder.newInstance()
                        .newState(ParticipantContextState.DEACTIVATED)
                        .participantId(participantId)
                        .build())
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .build());

        verify(publisherMock).unpublish(eq(did));
    }

    @SuppressWarnings("unchecked")
    @Test
    void onParticipantContextUpdated_whenDeactivated_notPublished_shouldBeNoop() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        var participantId = "test-id";
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.GENERATED).document(doc).build();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(didResource);
        when(didResourceStoreMock.query(any())).thenReturn(List.of(didResource));
        when(publisherMock.unpublish(anyString())).thenReturn(Result.success());

        service.on(EventEnvelope.Builder.newInstance()
                .payload(ParticipantContextUpdated.Builder.newInstance()
                        .newState(ParticipantContextState.DEACTIVATED)
                        .participantId(participantId)
                        .build())
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .build());

        verifyNoInteractions(publisherMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    void onParticipantContextUpdated_whenDeactivated_published_shouldBeNoop() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        var participantId = "test-id";
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(didResource);
        when(didResourceStoreMock.query(any())).thenReturn(List.of(didResource));
        when(publisherMock.unpublish(anyString())).thenReturn(Result.success());

        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_ID)
                .apiTokenAlias("token")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        service.on(EventEnvelope.Builder.newInstance()
                .payload(ParticipantContextUpdated.Builder.newInstance()
                        .newState(ParticipantContextState.DEACTIVATED)
                        .participantId(participantId)
                        .build())
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .build());

        verify(publisherMock).unpublish(eq(did));
    }

    @SuppressWarnings("unchecked")
    @Test
    void onParticipantContextUpdated_whenActivated_shouldPublish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        var participantId = "test-id";
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.GENERATED).document(doc).build();
        when(didResourceStoreMock.findById(eq(did))).thenReturn(didResource);
        when(didResourceStoreMock.query(any())).thenReturn(List.of(didResource));
        when(publisherMock.publish(anyString())).thenReturn(Result.success());

        service.on(EventEnvelope.Builder.newInstance()
                .payload(ParticipantContextUpdated.Builder.newInstance()
                        .newState(ParticipantContextState.ACTIVATED)
                        .participantId(participantId)
                        .build())
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .build());

        verify(publisherMock).publish(eq(did));
    }

    @SuppressWarnings("unchecked")
    @Test
    void onKeyPairActivated() throws JOSEException {
        var keyId = "key-id";
        var key = new ECKeyGenerator(Curve.P_256).keyID(keyId).generate();
        var doc = createDidDocument().build();
        var did = doc.getId();
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.GENERATED).document(doc).build();

        when(didResourceStoreMock.query(any(QuerySpec.class))).thenReturn(List.of(didResource));
        when(didResourceStoreMock.update(any())).thenReturn(StoreResult.success());

        var event = EventEnvelope.Builder.newInstance()
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .payload(KeyPairActivated.Builder.newInstance()
                        .keyId(keyId)
                        .keyPairResourceId("test-resource-id")
                        .participantId("test-participant")
                        .publicKey(key.toPublicJWK().toJSONString(), JSON_WEB_KEY_2020)
                        .build())
                .build();

        service.on(event);

        verify(didResourceStoreMock).query(any(QuerySpec.class));
        verify(didResourceStoreMock).update(argThat(dr -> dr.getDocument().getVerificationMethod().stream().anyMatch(vm -> vm.getId().equals(keyId))));
        verifyNoMoreInteractions(didResourceStoreMock);
        verifyNoInteractions(publisherMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    void onKeyPairRevoked() throws JOSEException {
        var keyId = "key-id";
        var doc = createDidDocument().verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id(keyId)
                        .publicKeyJwk(new ECKeyGenerator(Curve.P_256).keyID(keyId).generate().toJSONObject())
                        .build()))
                .build();
        var did = doc.getId();
        var didResource = DidResource.Builder.newInstance().did(did).state(DidState.GENERATED).document(doc).build();

        when(didResourceStoreMock.query(any(QuerySpec.class))).thenReturn(List.of(didResource));
        when(didResourceStoreMock.update(any())).thenReturn(StoreResult.success());

        var event = EventEnvelope.Builder.newInstance()
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .payload(KeyPairRevoked.Builder.newInstance()
                        .keyId(keyId)
                        .keyPairResourceId("test-resource-id")
                        .participantId("test-participant")
                        .build())
                .build();

        service.on(event);

        verify(didResourceStoreMock).query(any(QuerySpec.class));
        // assert that the DID Doc does not contain a VerificationMethod with the ID that was revoked
        verify(didResourceStoreMock).update(argThat(dr -> dr.getDocument().getVerificationMethod().stream().noneMatch(vm -> vm.getId().equals(keyId))));
        verifyNoMoreInteractions(didResourceStoreMock);
        verifyNoInteractions(publisherMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    void onOtherEvent_shouldLogWarning() {
        service.on(EventEnvelope.Builder.newInstance()
                .at(System.currentTimeMillis())
                .id(UUID.randomUUID().toString())
                .payload(new Event() {
                    @Override
                    public String name() {
                        return "TestEvent";
                    }
                })
                .build());
        verify(monitorMock).warning(startsWith("Received event with unexpected payload type: "));
    }

    private DidDocument.Builder createDidDocument() {
        return DidDocument.Builder.newInstance()
                .id(TEST_DID)
                .service(List.of(new Service("test-service", "test-service", "https://test.service.com/")))
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id(TEST_DID + "#key-1")
                        .publicKeyMultibase("saflasjdflaskjdflasdkfj")
                        .build()));
    }
}