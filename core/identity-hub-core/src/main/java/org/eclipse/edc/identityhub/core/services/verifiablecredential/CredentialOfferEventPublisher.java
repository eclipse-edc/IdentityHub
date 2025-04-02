/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferListener;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;

public class CredentialOfferEventPublisher implements CredentialOfferListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public CredentialOfferEventPublisher(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void received(CredentialOffer credentialOffer) {
        var evt = CredentialOfferReceived.Builder.newInstance()
                .id(credentialOffer.getId())
                .issuer(credentialOffer.issuer())
                .participantContextId(credentialOffer.getParticipantContextId())
                .build();
        publish(evt);
    }

    @SuppressWarnings("unchecked")
    private void publish(CredentialOfferReceived evt) {
        eventRouter.publish(EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(evt)
                .build());
    }
}
