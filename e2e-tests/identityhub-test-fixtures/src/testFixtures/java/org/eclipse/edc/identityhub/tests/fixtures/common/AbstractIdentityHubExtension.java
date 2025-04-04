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

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.system.configuration.Config;

import java.net.URI;
import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Base class for identity components such as Identity Hub, Issuer service.
 * Provides base endpoint for the identity service and the DID service.
 */
public abstract class AbstractIdentityHubExtension extends ComponentExtension {

    protected final LazySupplier<Endpoint> didEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://localhost:" + getFreePort() + "/"), Map.of()));
    protected final LazySupplier<Endpoint> identityEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/identity"), Map.of()));

    protected AbstractIdentityHubExtension(EmbeddedRuntime runtime) {
        super(runtime);
    }

    public abstract Config getConfiguration();

    public Endpoint getIdentityEndpoint() {
        return identityEndpoint.get();
    }


    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", didEndpoint.get().getUrl().getHost(), didEndpoint.get().getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

    public abstract static class Builder<P extends AbstractIdentityHubExtension, B extends Builder<P, B>> extends ComponentExtension.Builder<P, B> {

        protected Builder() {
        }

    }
}