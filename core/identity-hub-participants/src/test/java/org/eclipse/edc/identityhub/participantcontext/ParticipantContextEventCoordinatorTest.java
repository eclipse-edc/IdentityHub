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

import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ParticipantContextEventCoordinatorTest {
    private final Monitor monitor = mock();
    private final DidDocumentService didDocumentService = mock();
    private final KeyPairService keyPairService = mock();
    private final ParticipantContextEventCoordinator coordinator = new ParticipantContextEventCoordinator(monitor, didDocumentService, keyPairService);

    @Test
    void onParticipantCreated() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(didDocumentService.publish(anyString())).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.success());

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantId(participantId)
                .manifest(createManifest().build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(didDocumentService).publish(eq("did:web:" + participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verifyNoMoreInteractions(keyPairService, didDocumentService, monitor);
    }

    @Test
    void onParticipantCreated_didDocumentServiceStoreFailure() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.badRequest("foobar"));

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantId(participantId)
                .manifest(createManifest().build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(monitor).warning("foobar");
        verifyNoMoreInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onParticipantCreated_didDocumentServicePublishFailure() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(didDocumentService.publish(anyString())).thenReturn(ServiceResult.badRequest("foobar"));

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantId(participantId)
                .manifest(createManifest().build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(didDocumentService).publish(anyString());
        verify(monitor).warning("foobar");
        verifyNoMoreInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onParticipantCreated_keyPairServiceFailure() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(didDocumentService.publish(anyString())).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.notFound("foobar"));

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantId(participantId)
                .manifest(createManifest().build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(didDocumentService).publish(eq("did:web:" + participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verify(monitor).warning("foobar");
        verifyNoMoreInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onOtherEvent_expectWarning() {
        coordinator.on(envelope(new Event() {
            @Override
            public String name() {
                return "another.event";
            }
        }));

        verify(monitor).warning(startsWith("Received event with unexpected payload type:"));
        verifyNoMoreInteractions(monitor, didDocumentService, keyPairService);
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<Event> envelope(Event event) {
        return EventEnvelope.Builder.newInstance()
                .at(Instant.now().toEpochMilli())
                .payload(event)
                .build();
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
                .keyGeneratorParams(Map.of("algorithm", "EC"));
    }
}