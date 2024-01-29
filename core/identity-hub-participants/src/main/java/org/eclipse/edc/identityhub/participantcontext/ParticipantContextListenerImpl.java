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

import org.eclipse.edc.identityhub.spi.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextEvent;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextListener;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;

public class ParticipantContextListenerImpl implements ParticipantContextListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public ParticipantContextListenerImpl(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(ParticipantContext newContext, ParticipantManifest manifest) {
        var event = ParticipantContextCreated.Builder.newInstance()
                .participantId(newContext.getParticipantId())
                .manifest(manifest)
                .build();
        publish(event);
    }

    @Override
    public void deleted(ParticipantContext deletedContext) {
        var event = ParticipantContextDeleted.Builder.newInstance()
                .participantId(deletedContext.getParticipantId())
                .build();
        publish(event);
    }

    @Override
    public void updated(ParticipantContext updatedContext) {
        var event = ParticipantContextUpdated.Builder.newInstance()
                .participantId(updatedContext.getParticipantId())
                .newState(updatedContext.getStateAsEnum())
                .build();
        publish(event);
    }

    private void publish(ParticipantContextEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
