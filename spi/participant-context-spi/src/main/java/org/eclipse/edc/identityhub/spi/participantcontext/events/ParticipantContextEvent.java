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

package org.eclipse.edc.identityhub.spi.participantcontext.events;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.telemetry.TraceCarrier;

import java.util.Map;
import java.util.Objects;

/**
 * Base class for all events related to state changes and actions of {@link IdentityHubParticipantContext}s
 */
public abstract class ParticipantContextEvent extends Event implements TraceCarrier {
    protected String participantContextId;
    protected Map<String, String> traceContext;

    public String getParticipantContextId() {
        return participantContextId;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return traceContext;
    }

    public abstract static class Builder<T extends ParticipantContextEvent, B extends ParticipantContextEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public T build() {
            Objects.requireNonNull((event.participantContextId));
            event.traceContext = new Telemetry(GlobalOpenTelemetry.get()).getCurrentTraceContext();
            return event;
        }
    }
}
