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

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.identityhub.spi.did.events.DidDocumentObservable;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.publisher.did.local.TestFunctions.createDidResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LocalDidPublisherTest {

    public static final String DID = "did:web:test";
    private final DidResourceStore storeMock = mock();
    private final DidDocumentObservable observableMock = mock();
    private LocalDidPublisher publisher;
    private Monitor monitor;

    @BeforeEach
    void setUp() {
        monitor = mock();
        publisher = new LocalDidPublisher(observableMock, storeMock, monitor);
    }


    @ParameterizedTest
    @ValueSource(strings = {DID, "DID:web:test", "DID:WEB:TEST"})
    void canHandle(String validDid) {
        assertThat(publisher.canHandle(validDid)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:web", "DID:web:", "did:indy:whatever", "dod:web:something"})
    void canHandle_invalid(String validDid) {
        assertThat(publisher.canHandle(validDid)).isFalse();
    }

    @Test
    void publish_success() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource().build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        AbstractResultAssert.assertThat(publisher.publish(DID)).isSucceeded();

        verify(storeMock).findById(anyString());
        verify(storeMock).update(argThat(dr -> dr.getState() == DidState.PUBLISHED.code()));
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void publish_notExists_returnsFailure() {
        when(storeMock.findById(any())).thenReturn(null);

        AbstractResultAssert.assertThat(publisher.publish("did:web:foo")).isFailed()
                .detail()
                .isEqualTo("A DID Resource with the ID 'did:web:foo' was not found.");

        verify(storeMock).findById(anyString());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void publish_alreadyPublished_expectWarning() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource()
                .state(DidState.PUBLISHED)
                .build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        AbstractResultAssert.assertThat(publisher.publish(DID)).isSucceeded();

        verify(storeMock).findById(anyString());
        verify(storeMock).update(any());
        verify(monitor).warning("DID 'did:web:test' is already published - this action will overwrite it.");
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void publish_storeFailsUpdate_returnsFailure() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource().build());
        when(storeMock.update(any())).thenReturn(StoreResult.duplicateKeys("test error"));

        AbstractResultAssert.assertThat(publisher.publish(DID)).isFailed()
                .detail()
                .isEqualTo("test error");

        verify(storeMock).findById(anyString());
        verify(storeMock).update(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void unpublish_success() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource()
                .state(DidState.PUBLISHED)
                .build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        AbstractResultAssert.assertThat(publisher.unpublish(DID)).isSucceeded();

        verify(storeMock).findById(anyString());
        verify(storeMock).update(argThat(dr -> dr.getState() == DidState.UNPUBLISHED.code()));
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void unpublish_notExists_returnsFailure() {
        when(storeMock.findById(anyString())).thenReturn(null);

        AbstractResultAssert.assertThat(publisher.unpublish(DID)).isFailed()
                .detail()
                .contains("A DID Resource with the ID 'did:web:test' was not found.");

        verify(storeMock).findById(anyString());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void unpublish_notPublished_expectWarning() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource()
                .state(DidState.UNPUBLISHED)
                .build());
        when(storeMock.update(any())).thenReturn(StoreResult.success());

        AbstractResultAssert.assertThat(publisher.unpublish(DID)).isSucceeded();

        verify(storeMock).findById(anyString());
        verify(storeMock).update(any());
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }

    @Test
    void unpublish_storeFailsUpdate_returnsFailure() {
        when(storeMock.findById(anyString())).thenReturn(createDidResource()
                .state(DidState.PUBLISHED)
                .build());
        when(storeMock.update(any())).thenReturn(StoreResult.notFound("foobar"));

        AbstractResultAssert.assertThat(publisher.unpublish(DID)).isFailed()
                .detail()
                .isEqualTo("foobar");

        verify(storeMock).findById(anyString());
        verify(storeMock).update(any());
        verifyNoMoreInteractions(storeMock, observableMock);
    }
}