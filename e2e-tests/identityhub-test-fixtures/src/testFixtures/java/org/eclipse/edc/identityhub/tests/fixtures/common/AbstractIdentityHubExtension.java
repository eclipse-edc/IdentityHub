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

    protected final LazySupplier<Endpoint> didEndpoint;
    protected final LazySupplier<Endpoint> identityEndpoint;
    protected final LazySupplier<Endpoint> stsEndpoint;

    protected AbstractIdentityHubExtension(EmbeddedRuntime runtime) {
        this(runtime, "localhost");
    }

    protected AbstractIdentityHubExtension(EmbeddedRuntime runtime, String host) {
        super(runtime);
        didEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/".formatted(host, getFreePort())), Map.of()));
        identityEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/identity".formatted(host, getFreePort())), Map.of()));
        stsEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/sts".formatted(host, getFreePort())), Map.of()));
    }

    public Endpoint getStsEndpoint() {
        return stsEndpoint.get();
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
        protected String host = null;

        protected Builder() {
        }

        /**
         * Override the host name. By default, this is {@code localhost}, but in some circumstances it may be necessary to override that,
         * for example in some scenarios involving Docker networking
         *
         * @param host The hostname
         */
        public B host(String host) {
            this.host = host;
            return self();
        }

    }
}