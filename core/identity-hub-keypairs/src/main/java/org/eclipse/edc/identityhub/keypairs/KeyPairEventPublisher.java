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

package org.eclipse.edc.identityhub.keypairs;

import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEvent;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEventListener;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;

public class KeyPairEventPublisher implements KeyPairEventListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public KeyPairEventPublisher(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void added(KeyPairResource keyPair, String type) {
        var event = KeyPairAdded.Builder.newInstance()
                .participantId(keyPair.getParticipantId())
                .keyPairResource(keyPair)
                .keyId(keyPair.getKeyId())
                .publicKey(keyPair.getSerializedPublicKey(), type)
                .build();
        publish(event);
    }

    @Override
    public void rotated(KeyPairResource keyPair, @Nullable KeyDescriptor newKeyDesc) {
        var event = KeyPairRotated.Builder.newInstance()
                .participantId(keyPair.getParticipantId())
                .keyPairResource(keyPair)
                .keyId(keyPair.getKeyId())
                .newKeyDescriptor(newKeyDesc)
                .build();
        publish(event);
    }

    @Override
    public void revoked(KeyPairResource keyPair, @Nullable KeyDescriptor newKeyDesc) {
        var event = KeyPairRevoked.Builder.newInstance()
                .participantId(keyPair.getParticipantId())
                .keyPairResource(keyPair)
                .keyId(keyPair.getKeyId())
                .newKeyDescriptor(newKeyDesc)
                .build();
        publish(event);
    }

    @Override
    public void activated(KeyPairResource activatedKeyPair, String type) {
        var event = KeyPairActivated.Builder.newInstance()
                .participantId(activatedKeyPair.getParticipantId())
                .keyPairResource(activatedKeyPair)
                .publicKey(activatedKeyPair.getSerializedPublicKey(), type)
                .keyId(activatedKeyPair.getKeyId())
                .build();
        publish(event);
    }

    private void publish(KeyPairEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
