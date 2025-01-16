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

import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleting;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextEvent;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextListener;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;

public class ParticipantContextEventPublisher implements ParticipantContextListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public ParticipantContextEventPublisher(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(ParticipantContext newContext, ParticipantManifest manifest) {
        var event = ParticipantContextCreated.Builder.newInstance()
                .participantContextId(newContext.getParticipantContextId())
                .manifest(manifest)
                .build();
        publish(event);
    }

    @Override
    public void updated(ParticipantContext updatedContext) {
        var event = ParticipantContextUpdated.Builder.newInstance()
                .participantContextId(updatedContext.getParticipantContextId())
                .newState(updatedContext.getStateAsEnum())
                .build();
        publish(event);
    }

    @Override
    public void deleting(ParticipantContext deletedContext) {
        var event = ParticipantContextDeleting.Builder.newInstance()
                .participantContextId(deletedContext.getParticipantContextId())
                .participant(deletedContext)
                .build();
        publish(event);
    }

    @Override
    public void deleted(ParticipantContext deletedContext) {
        var event = ParticipantContextDeleted.Builder.newInstance()
                .participantContextId(deletedContext.getParticipantContextId())
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
