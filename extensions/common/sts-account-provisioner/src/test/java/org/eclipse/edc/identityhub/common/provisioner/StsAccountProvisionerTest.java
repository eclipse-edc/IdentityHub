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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StsAccountProvisionerTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String PARTICIPANT_DID = "did:web:" + PARTICIPANT_CONTEXT_ID;
    private static final String KEY_ID = "test-key-id";
    private final KeyPairService keyPairService = mock();
    private final DidDocumentService didDocumentService = mock();
    private final StsAccountStore stsAccountStore = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();
    private final StsClientSecretGenerator stsClientSecretGenerator = parameters -> UUID.randomUUID().toString();
    private final StsAccountProvisioner accountProvisioner = new StsAccountProvisioner(monitor, stsAccountStore, vault, stsClientSecretGenerator, new NoopTransactionContext());

    @Test
    void create() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.success(createStsClient().build()));
        when(vault.storeSecret(anyString(), anyString())).thenReturn(Result.success());

        assertThat(accountProvisioner.create(createManifest().build())).isSucceeded();

        verify(stsAccountStore).create(any());
        verify(vault).storeSecret(anyString(), argThat(secret -> UUID.fromString(secret) != null));
        verifyNoInteractions(keyPairService, didDocumentService);
    }

    @Test
    void create_whenClientAlreadyExists() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var res = accountProvisioner.create(createManifest().build());
        assertThat(res).isFailed()
                .detail().isEqualTo("foo");

        verify(stsAccountStore).create(any());
        verifyNoInteractions(keyPairService, didDocumentService, vault);
    }

    @Test
    void onKeyRevoked_shouldUpdate() {
        when(stsAccountStore.findById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success(createStsClient().build()));
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.success(a.getArguments()[0]));
        accountProvisioner.on(event(KeyPairRevoked.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .keyPairResource(KeyPairResource.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .keyId(KEY_ID)
                .build()));

        verify(stsAccountStore).findById(PARTICIPANT_CONTEXT_ID);
        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore, didDocumentService, keyPairService);
    }

    @Test
    void onKeyRotated_withNewKey_shouldUpdate() {
        when(stsAccountStore.findById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success(createStsClient().build()));
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.success(a.getArguments()[0]));

        accountProvisioner.on(event(KeyPairRotated.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .keyPairResource(KeyPairResource.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .keyId(KEY_ID)
                .build()));

        verify(stsAccountStore).findById(PARTICIPANT_CONTEXT_ID);
        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore, didDocumentService, keyPairService);
    }

    @Test
    void onParticipantDeleted_shouldDelete() {
        when(stsAccountStore.deleteById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success());
        accountProvisioner.on(event(ParticipantContextDeleted.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .build()));

        verify(stsAccountStore).deleteById(PARTICIPANT_CONTEXT_ID);
        verifyNoMoreInteractions(keyPairService, didDocumentService, stsAccountStore);
    }

    @Test
    void onOtherEvent_shouldLogWarning() {
        accountProvisioner.on(event(new DummyEvent()));
        verify(monitor).warning(startsWith("Received event with unexpected payload"));
        verifyNoInteractions(keyPairService, didDocumentService, stsAccountStore, vault);
    }

    private StsAccount.Builder createStsClient() {
        return StsAccount.Builder.newInstance()
                .id("test-id")
                .name("test-name")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .secretAlias("test-secret")
                .publicKeyReference("public-key-ref")
                .privateKeyAlias("private-key-alias")
                .clientId("client-id");
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
