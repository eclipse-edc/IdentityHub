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

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentEvent;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentListener;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentUnpublished;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;

public class DidDocumentListenerImpl implements DidDocumentListener {

    private final Clock clock;
    private final EventRouter eventRouter;

    public DidDocumentListenerImpl(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void published(DidDocument document, String participantId) {
        var event = DidDocumentPublished.Builder.newInstance()
                .participantContextId(participantId)
                .did(document.getId())
                .build();
        publish(event);
    }

    @Override
    public void unpublished(DidDocument document, String participantId) {
        var event = DidDocumentUnpublished.Builder.newInstance()
                .participantContextId(participantId)
                .did(document.getId())
                .build();
        publish(event);
    }

    private void publish(DidDocumentEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
