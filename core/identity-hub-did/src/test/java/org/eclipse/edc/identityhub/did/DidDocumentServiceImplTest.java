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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisher;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.did.spi.model.DidResource;
import org.eclipse.edc.identithub.did.spi.model.DidState;
import org.eclipse.edc.identithub.did.spi.store.DidResourceStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DidDocumentServiceImplTest {
    private final DidResourceStore storeMock = mock();
    private final DidDocumentPublisherRegistry publisherRegistry = mock();
    private final DidDocumentPublisher publisherMock = mock();
    private DidDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        var trx = new NoopTransactionContext();
        when(publisherRegistry.getPublisher(startsWith("did:web:"))).thenReturn(publisherMock);

        service = new DidDocumentServiceImpl(trx, storeMock, publisherRegistry);
    }

    @Test
    void publish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(publisherMock.publish(did)).thenReturn(Result.success());

        assertThat(service.publish(did)).isSucceeded();

        verify(storeMock).findById(did);
        verify(publisherMock).publish(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void publish_notExist() {
        var did = "did:web:test-did";
        when(storeMock.findById(eq(did))).thenReturn(null);

        assertThat(service.publish(did)).isFailed()
                .detail().isEqualTo(service.notFoundMessage(did));

        verify(storeMock).findById(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void publish_noPublisherFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(publisherRegistry.getPublisher(any())).thenReturn(null);
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        assertThat(service.publish(did)).isFailed().detail()
                .isEqualTo(service.noPublisherFoundMessage(did));

        verify(storeMock).findById(did);
        verify(publisherRegistry).getPublisher(did);
        verifyNoMoreInteractions(publisherMock, storeMock, publisherRegistry);
    }

    @Test
    void publish_publisherReportsError() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(publisherMock.publish(did)).thenReturn(Result.failure("test-failure"));

        assertThat(service.publish(did)).isFailed()
                .detail()
                .isEqualTo("test-failure");

        verify(storeMock).findById(did);
        verify(publisherMock).publish(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void unpublish() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());
        when(publisherMock.unpublish(did)).thenReturn(Result.success());

        assertThat(service.unpublish(did)).isSucceeded();

        verify(storeMock).findById(did);
        verify(publisherMock).unpublish(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void unpublish_notExist() {
        var did = "did:web:test-did";
        when(storeMock.findById(eq(did))).thenReturn(null);

        assertThat(service.unpublish(did)).isFailed()
                .detail().isEqualTo(service.notFoundMessage(did));

        verify(storeMock).findById(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void unpublish_noPublisherFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(publisherRegistry.getPublisher(any())).thenReturn(null);
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());

        assertThat(service.unpublish(did)).isFailed().detail()
                .isEqualTo(service.noPublisherFoundMessage(did));

        verify(storeMock).findById(did);
        verify(publisherRegistry).getPublisher(did);
        verifyNoMoreInteractions(publisherMock, storeMock, publisherRegistry);
    }

    @Test
    void unpublish_publisherReportsError() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).state(DidState.PUBLISHED).document(doc).build());
        when(publisherMock.unpublish(did)).thenReturn(Result.failure("test-failure"));

        assertThat(service.unpublish(did)).isFailed()
                .detail()
                .isEqualTo("test-failure");

        verify(storeMock).findById(did);
        verify(publisherMock).unpublish(did);
        verifyNoMoreInteractions(publisherMock, storeMock);
    }

    @Test
    void queryDocuments() {
        var q = QuerySpec.max();
        var doc = createDidDocument().build();
        var res = DidResource.Builder.newInstance().did(doc.getId()).state(DidState.PUBLISHED).document(doc).build();
        when(storeMock.query(any())).thenReturn(List.of(res));

        assertThat(service.queryDocuments(q)).isSucceeded();

        verify(storeMock).query(eq(q));
        verifyNoMoreInteractions(publisherMock, storeMock, publisherRegistry);
    }

    @Test
    void addEndpoint() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());
        var res = service.addService(did, new Service("new-id", "test-type", "https://test.com"));
        assertThat(res).isSucceeded();

        verify(storeMock).findById(eq(did));
        verify(storeMock).update(any());
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void addEndpoint_alreadyExists() {
        var newService = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(newService)).build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        var res = service.addService(did, newService);
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' already contains a service endpoint with ID 'new-id'.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void addEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(null);
        var res = service.addService(did, new Service("test-id", "test-type", "https://test.com"));
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void replaceEndpoint() {
        var toReplace = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(toReplace)).build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        var res = service.replaceService(did, toReplace);
        assertThat(res).isSucceeded();

        verify(storeMock).findById(eq(did));
        verify(storeMock).update(any());
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void replaceEndpoint_doesNotExist() {
        var replace = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        var res = service.replaceService(did, replace);
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' does not contain a service endpoint with ID 'new-id'.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void replaceEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(null);
        var res = service.replaceService(did, new Service("test-id", "test-type", "https://test.com"));
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void removeEndpoint() {
        var toRemove = new Service("new-id", "test-type", "https://test.com");
        var doc = createDidDocument().service(List.of(toRemove)).build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        var res = service.removeService(did, toRemove.getId());
        assertThat(res).isSucceeded();

        verify(storeMock).findById(eq(did));
        verify(storeMock).update(any());
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void removeEndpoint_doesNotExist() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(DidResource.Builder.newInstance().did(did).document(doc).build());

        var res = service.removeService(did, "not-exist-id");
        assertThat(res).isFailed()
                .detail().isEqualTo("DID 'did:web:testdid' does not contain a service endpoint with ID 'not-exist-id'.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }

    @Test
    void removeEndpoint_didNotFound() {
        var doc = createDidDocument().build();
        var did = doc.getId();
        when(storeMock.findById(eq(did))).thenReturn(null);
        var res = service.removeService(did, "does-not-matter-id");
        assertThat(res).isFailed()
                .detail()
                .isEqualTo("DID 'did:web:testdid' not found.");

        verify(storeMock).findById(eq(did));
        verifyNoMoreInteractions(storeMock, publisherMock);
    }


    private DidDocument.Builder createDidDocument() {
        return DidDocument.Builder.newInstance()
                .id("did:web:testdid")
                .service(List.of(new Service("test-service", "test-service", "https://test.service.com/")))
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id("did:web:testdid#key-1")
                        .publicKeyMultibase("saflasjdflaskjdflasdkfj")
                        .build()));
    }
}