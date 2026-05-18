/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.issuance.events;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceEvent;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceEventListener;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceRejected;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceRequested;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;
import java.util.Collection;

public class IssuanceEventPublisher implements IssuanceEventListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public IssuanceEventPublisher(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void received(IssuanceProcess ip) {
        var event = IssuanceRequested.Builder.newInstance()
                .holderProcessId(ip.getHolderId())
                .holderProcessId(ip.getHolderPid())
                .issuerParticipantContextId(ip.getParticipantContextId())
                .credentialDefinitionIds(ip.getCredentialDefinitions())
                .credentialFormats(ip.getCredentialFormats())
                .build();

        publishEvent(event);
    }

    @Override
    public void rejected(String holderPid, String issuerParticipantContextId, String failureDetail) {
        var event = IssuanceRejected.Builder.newInstance()
                .holderId(holderPid)
                .issuerParticipantContextId(issuerParticipantContextId)
                .reason(failureDetail)
                .build();
        publishEvent(event);
    }

    @Override
    public void approved(IssuanceProcess process) {
        IssuanceEventListener.super.approved(process);
    }

    @Override
    public void generated(IssuanceProcess process, Collection<VerifiableCredentialContainer> creds) {
        IssuanceEventListener.super.generated(process, creds);
    }

    @Override
    public void delivered(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {
        IssuanceEventListener.super.delivered(process, credentials);
    }

    @Override
    public void errored(IssuanceProcess process, Throwable throwable) {
        IssuanceEventListener.super.errored(process, throwable);
    }

    @SuppressWarnings("unchecked")
    private void publishEvent(IssuanceEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
