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

import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class IdentityHubParticipantContextEventCoordinatorTest {
    private final Monitor monitor = mock();
    private final DidDocumentService didDocumentService = mock();
    private final KeyPairService keyPairService = mock();
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final ParticipantContextEventCoordinator coordinator = new ParticipantContextEventCoordinator(monitor, didDocumentService, keyPairService, participantContextService, new Telemetry());

    @BeforeEach
    void setup() {
        when(participantContextService.updateParticipant(anyString(), any()))
                .thenReturn(ServiceResult.success());
    }

    @Test
    void onParticipantCreated() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(didDocumentService.publish(anyString())).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.success());

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verifyNoMoreInteractions(keyPairService, didDocumentService, monitor);
    }

    @Test
    void onParticipantCreated_didDocumentServiceStoreFailure_shouldRollBack() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.badRequest("foobar"));

        assertThatThrownBy(() -> coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().build())
                .build())))
                .isInstanceOf(ProvisioningException.class)
                .hasMessage("foobar");

        verify(didDocumentService).store(any(), eq(participantId));
        verifyNoMoreInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onParticipantCreated_active_didDocumentServicePublishFailure() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.success());

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().active(true).build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verifyNoMoreInteractions(didDocumentService);
    }

    @Test
    void onParticipantCreated_notActive_shouldNotPublish() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.success());

        coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().active(false).build())
                .build()));

        verify(didDocumentService).store(any(), eq(participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verify(didDocumentService, never()).publish(anyString());
        verifyNoMoreInteractions(didDocumentService);
    }

    @Test
    void onParticipantCreated_active_whenKeyPairServiceFailure_shouldRollBack() {
        var participantId = "test-id";
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(KeyDescriptor.class), anyBoolean())).thenReturn(ServiceResult.notFound("foobar"));

        assertThatThrownBy(() -> coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().active(true).build())
                .build())))
                .isInstanceOf(ProvisioningException.class)
                .hasMessage("foobar");

        verify(didDocumentService).store(any(), eq(participantId));
        verify(keyPairService).addKeyPair(eq(participantId), any(), eq(true));
        verify(didDocumentService, never()).publish(eq("did:web:" + participantId));
        verify(participantContextService, never()).updateParticipant(anyString(), any());
        // the failed key pair was not added, so its key material is not discarded
        verifyNoMoreInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onParticipantCreated_whenSecondKeyPairFails_shouldDiscardFirstAndRollBack() {
        var participantId = "test-id";
        var firstKey = createKey().keyId("key1").privateKeyAlias("alias1").build();
        var secondKey = createKey().keyId("key2").privateKeyAlias("alias2").build();
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(participantId, firstKey, true)).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(participantId, secondKey, true)).thenReturn(ServiceResult.conflict("foobar"));

        assertThatThrownBy(() -> coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().keys(new LinkedHashSet<>(List.of(firstKey, secondKey))).build())
                .build())))
                .isInstanceOf(ProvisioningException.class)
                .hasMessage("foobar");

        verify(keyPairService).discardKeyMaterial(participantId, firstKey);
        verify(keyPairService, never()).discardKeyMaterial(participantId, secondKey);
        verify(participantContextService, never()).updateParticipant(anyString(), any());
    }

    @Test
    void onParticipantCreated_whenKeyPairServiceThrows_shouldDiscardAddedKeysAndRethrow() {
        var participantId = "test-id";
        var firstKey = createKey().keyId("key1").privateKeyAlias("alias1").build();
        var secondKey = createKey().keyId("key2").privateKeyAlias("alias2").build();
        var exception = new EdcException("foobar");
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(participantId, firstKey, true)).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(participantId, secondKey, true)).thenThrow(exception);

        assertThatThrownBy(() -> coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(createManifest().keys(new LinkedHashSet<>(List.of(firstKey, secondKey))).build())
                .build())))
                .isSameAs(exception);

        verify(keyPairService).discardKeyMaterial(participantId, firstKey);
        verify(keyPairService, never()).discardKeyMaterial(participantId, secondKey);
    }

    @Test
    void onParticipantCreated_active_whenActivationFails_shouldDiscardKeysAndRollBack() {
        var participantId = "test-id";
        var manifest = createManifest().active(true).build();
        when(didDocumentService.store(any(), eq(participantId))).thenReturn(ServiceResult.success());
        when(keyPairService.addKeyPair(eq(participantId), any(), anyBoolean())).thenReturn(ServiceResult.success());
        when(participantContextService.updateParticipant(eq(participantId), any())).thenReturn(ServiceResult.unexpected("foobar"));

        assertThatThrownBy(() -> coordinator.on(envelope(ParticipantContextCreated.Builder.newInstance()
                .participantContextId(participantId)
                .manifest(manifest)
                .build())))
                .isInstanceOf(ProvisioningException.class)
                .hasMessage("foobar");

        verify(keyPairService).discardKeyMaterial(participantId, manifest.getKeys().iterator().next());
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
                .participantContextId("test-id");
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance().keyId("test-kie")
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .privateKeyAlias("private-alias")
                .keyGeneratorParams(Map.of("algorithm", "EC"));
    }
}