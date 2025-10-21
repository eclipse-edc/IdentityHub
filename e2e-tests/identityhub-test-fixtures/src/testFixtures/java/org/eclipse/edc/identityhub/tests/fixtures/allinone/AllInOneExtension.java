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

package org.eclipse.edc.identityhub.tests.fixtures.allinone;

import org.eclipse.edc.identityhub.tests.fixtures.common.ComponentExtension;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.identityhub.tests.fixtures.common.LazySupplier;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Test extension for the Issuer Service. Provides a default configuration for the Issuer Service
 * and can inject in test methods {@link AllInOneRuntime} instances.
 */
public class AllInOneExtension extends ComponentExtension {
    protected final LazySupplier<Endpoint> didEndpoint;
    protected final LazySupplier<Endpoint> identityEndpoint;
    protected final LazySupplier<Endpoint> stsEndpoint;
    protected final LazySupplier<Endpoint> adminEndpoint;
    protected final LazySupplier<Endpoint> issuerApiEndpoint;
    protected final LazySupplier<Endpoint> credentialsEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/credentials"), Map.of()));

    private final AllInOneRuntime allInOneRuntime;

    private AllInOneExtension(EmbeddedRuntime runtime) {
        this(runtime, "localhost");
    }

    private AllInOneExtension(EmbeddedRuntime runtime, String host) {
        super(runtime);

        didEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/".formatted(host, getFreePort())), Map.of()));
        identityEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/identity".formatted(host, getFreePort())), Map.of()));
        stsEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/sts".formatted(host, getFreePort())), Map.of()));
        allInOneRuntime = new AllInOneRuntime(this);
        adminEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/admin".formatted(host, getFreePort())), Map.of()));
        issuerApiEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://%s:%d/api/issuance".formatted(host, getFreePort())), Map.of()));
    }

    public Endpoint getIssuerApiEndpoint() {
        return issuerApiEndpoint.get();
    }

    public Endpoint getAdminEndpoint() {
        return adminEndpoint.get();
    }

    public Endpoint getCredentialsEndpoint() {
        return credentialsEndpoint.get();
    }

    public Endpoint getStsEndpoint() {
        return stsEndpoint.get();
    }

    @Override
    public Config getConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.issueradmin.port", String.valueOf(adminEndpoint.get().getUrl().getPort()));
                put("web.http.issueradmin.path", adminEndpoint.get().getUrl().getPath());
                put("web.http.sts.port", String.valueOf(stsEndpoint.get().getUrl().getPort()));
                put("web.http.sts.path", stsEndpoint.get().getUrl().getPath());
                put("web.http.identity.port", String.valueOf(identityEndpoint.get().getUrl().getPort()));
                put("web.http.identity.path", identityEndpoint.get().getUrl().getPath());
                put("web.http.issuance.port", String.valueOf(issuerApiEndpoint.get().getUrl().getPort()));
                put("web.http.issuance.path", issuerApiEndpoint.get().getUrl().getPath());
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/.well-known/api");
                put("web.http.did.port", String.valueOf(didEndpoint.get().getUrl().getPort()));
                put("web.http.did.path", didEndpoint.get().getUrl().getPath());

                put("web.http.statuslist.port", String.valueOf(getFreePort()));
                put("edc.sql.schema.autocreate", "true");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                put("edc.issuer.statuslist.signing.key.alias", "signing-key");
                // config for the embedded STS
                put("edc.iam.sts.publickey.id", "test-public-key");
                put("edc.iam.sts.privatekey.alias", "issuer-alias");
                put("edc.iam.did.web.use.https", "false");


                put("web.http.credentials.port", String.valueOf(credentialsEndpoint.get().getUrl().getPort()));
                put("web.http.credentials.path", String.valueOf(credentialsEndpoint.get().getUrl().getPath()));
                put("web.http.accounts.port", String.valueOf(getFreePort()));
                put("web.http.accounts.path", "/api/accounts");
                put("edc.runtime.id", name);

                put("edc.iam.credential.status.check.period", "5");
            }
        });
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isParameterSupported(parameterContext, AllInOneRuntime.class)) {
            return true;
        }

        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isParameterSupported(parameterContext, AllInOneRuntime.class)) {
            return allInOneRuntime;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    public Endpoint getIdentityEndpoint() {
        return identityEndpoint.get();
    }

    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", didEndpoint.get().getUrl().getHost(), didEndpoint.get().getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

    public static class Builder extends ComponentExtension.Builder<AllInOneExtension, Builder> {

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        protected AllInOneExtension internalBuild() {
            return new AllInOneExtension(runtime);
        }
    }
}
