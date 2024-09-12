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

package org.eclipse.edc.identityhub.common.provisioner;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identithub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identithub.spi.did.model.DidResource;
import org.eclipse.edc.identithub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StsAccountProvisionerTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String PARTICIPANT_DID = "did:web:" + PARTICIPANT_CONTEXT_ID;
    private static final String KEY_ID = "test-key-id";
    private final KeyPairService keyPairService = mock();
    private final DidDocumentService didDocumentService = mock();
    private final StsClientStore stsClientStore = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();
    private final StsAccountProvisioner accountProvisioner = new StsAccountProvisioner(monitor, keyPairService, didDocumentService, stsClientStore, vault);

    @Test
    void onParticipantCreated() {
        when(stsClientStore.create(any())).thenReturn(StoreResult.success());

        accountProvisioner.on(event(ParticipantContextCreated.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .manifest(createManifest().build())
                .build()));

        verify(stsClientStore).create(any());
        verifyNoInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onParticipantCreated_whenClientAlreadyExists() {
        when(stsClientStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        accountProvisioner.on(event(ParticipantContextCreated.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .manifest(createManifest().build())
                .build()));

        verify(monitor).warning(eq("foo"));
        verify(stsClientStore).create(any());
        verifyNoInteractions(keyPairService, didDocumentService);
    }

    @Test
    void onKeyRevoked_shouldUpdate() {
        accountProvisioner.on(event(KeyPairRevoked.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .keyPairResourceId(UUID.randomUUID().toString())
                .keyId(KEY_ID)
                .build()));

        verify(monitor).warning(contains("not yet implemented"));
        verifyNoInteractions(stsClientStore, didDocumentService, keyPairService);
    }

    @Test
    void onKeyRotated_withNewKey_shouldUpdate() {
        accountProvisioner.on(event(KeyPairRotated.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .keyPairResourceId(UUID.randomUUID().toString())
                .keyId(KEY_ID)
                .build()));

        verify(monitor).warning(contains("not yet implemented"));
        verifyNoInteractions(stsClientStore, didDocumentService, keyPairService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "key-1", PARTICIPANT_DID + "#key-1" })
    void onDidPublished_shouldUpdate(String keyId) {
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .keyId(keyId)
                .isDefaultPair(true)
                .privateKeyAlias(keyId + "-alias")
                .state(KeyPairState.ACTIVATED.code())
                .build())));

        var doc = DidDocument.Builder.newInstance()
                .id(PARTICIPANT_DID)
                .service(List.of(new Service("test-service", "test-service", "https://test.service.com/")))
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id(keyId)
                        .publicKeyMultibase("saflasjdflaskjdflasdkfj")
                        .controller(PARTICIPANT_DID)
                        .build()))
                .build();
        var res = DidResource.Builder.newInstance().did(doc.getId()).state(DidState.PUBLISHED).document(doc).build();
        when(didDocumentService.findById(eq(PARTICIPANT_DID))).thenReturn(res);

        accountProvisioner.on(event(DidDocumentPublished.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .did(PARTICIPANT_DID)
                .build()));

        verify(monitor).warning(contains("not yet implemented"));
        verify(keyPairService).query(any(QuerySpec.class));
        verify(didDocumentService).findById(eq(PARTICIPANT_DID));
        verifyNoInteractions(stsClientStore);
    }

    @Test
    void onDidPublished_noDefaultKey_shouldUpdate() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .isDefaultPair(false)
                .privateKeyAlias(KEY_ID + "-alias")
                .state(KeyPairState.ACTIVATED.code())
                .build())));
        accountProvisioner.on(event(DidDocumentPublished.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .did(PARTICIPANT_DID)
                .build()));

        verify(monitor).warning(contains("No default keypair found for participant " + PARTICIPANT_CONTEXT_ID));
        verify(keyPairService).query(any(QuerySpec.class));
        verifyNoInteractions(stsClientStore, didDocumentService);

    }

    @Test
    void onOtherEvent_shouldLogWarning() {
        accountProvisioner.on(event(new DummyEvent()));
        verify(monitor).warning(startsWith("Received event with unexpected payload"));
        verifyNoInteractions(keyPairService, didDocumentService, stsClientStore, vault);
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .active(true)
                .did(PARTICIPANT_DID)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(KEY_ID + "-alias")
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId(KEY_ID)
                        .build()
                );
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<Event> event(Event event) {
        return EventEnvelope.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();
    }

    private static class DummyEvent extends Event {
        @Override
        public String name() {
            return "dummy";
        }
    }
}