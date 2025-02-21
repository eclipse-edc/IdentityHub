/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures.common;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class AbstractRuntimeConfiguration {

    protected Endpoint didEndpoint;
    protected String id;
    protected String name;

    public Endpoint getDidEndpoint() {
        return didEndpoint;
    }


    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", didEndpoint.getUrl().getHost(), didEndpoint.getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

    public static class Builder<P extends AbstractRuntimeConfiguration, B extends AbstractRuntimeConfiguration.Builder<P, B>> {
        protected final P participant;

        protected Builder(P participant) {
            this.participant = participant;
        }

        public B id(String id) {
            this.participant.id = id;
            return self();
        }

        public B name(String name) {
            this.participant.name = name;
            return self();
        }

        public P build() {
            Objects.requireNonNull(participant.id, "id");
            Objects.requireNonNull(participant.name, "name");
            participant.didEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/"), Map.of());
            return participant;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
    }
}