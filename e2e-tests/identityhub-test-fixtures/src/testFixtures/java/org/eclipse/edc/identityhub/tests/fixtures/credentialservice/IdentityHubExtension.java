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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHubExtension;
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

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Test extension for the Identity Hub. Provides a default configuration for the Identity Hub
 * and can inject in test methods {@link IdentityHubRuntime} and {@link IdentityHubApiClient} instances.
 */
public class IdentityHubExtension extends AbstractIdentityHubExtension {

    protected final LazySupplier<Endpoint> credentialsEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/credentials"), Map.of()));
    protected final LazySupplier<Endpoint> stsEndpoint = new LazySupplier<>(() -> new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/sts"), Map.of()));
    protected final IdentityHubRuntime identityHubRuntime;
    protected final IdentityHubApiClient apiClient;

    private IdentityHubExtension(EmbeddedRuntime runtime) {
        super(runtime);
        identityHubRuntime = new IdentityHubRuntime(this);
        apiClient = new IdentityHubApiClient(this);
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
                put("web.http.credentials.port", String.valueOf(credentialsEndpoint.get().getUrl().getPort()));
                put("web.http.credentials.path", String.valueOf(credentialsEndpoint.get().getUrl().getPath()));
                put("web.http.identity.port", String.valueOf(identityEndpoint.get().getUrl().getPort()));
                put("web.http.identity.path", identityEndpoint.get().getUrl().getPath());
                put("web.http.sts.port", String.valueOf(stsEndpoint.get().getUrl().getPort()));
                put("web.http.sts.path", stsEndpoint.get().getUrl().getPath());
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/.well-known/version");
                put("web.http.accounts.port", String.valueOf(getFreePort()));
                put("web.http.accounts.path", "/api/accounts");
                put("web.http.did.port", String.valueOf(didEndpoint.get().getUrl().getPort()));
                put("web.http.did.path", didEndpoint.get().getUrl().getPath());
                put("edc.runtime.id", name);
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                put("edc.iam.sts.publickey.id", "test-public-key");
                put("edc.iam.sts.privatekey.alias", "user1-alias"); //this must be "username"-alias
                put("edc.iam.did.web.use.https", "false");
            }
        });
    }

    public Config stsConfig() {
        return ConfigFactory.fromMap(Map.of("edc.iam.sts.oauth.token.url", stsEndpoint.get().getUrl().toString() + "/token"));
    }

    public Endpoint getCredentialsEndpoint() {
        return credentialsEndpoint.get();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isParameterSupported(parameterContext, IdentityHubRuntime.class)) {
            return true;
        } else if (isParameterSupported(parameterContext, IdentityHubApiClient.class)) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isParameterSupported(parameterContext, IdentityHubRuntime.class)) {
            return identityHubRuntime;
        } else if (isParameterSupported(parameterContext, IdentityHubApiClient.class)) {
            return apiClient;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    public static class Builder extends AbstractIdentityHubExtension.Builder<IdentityHubExtension, Builder> {

        protected Builder() {
            super();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        protected IdentityHubExtension internalBuild() {
            return new IdentityHubExtension(runtime);
        }

    }
}
