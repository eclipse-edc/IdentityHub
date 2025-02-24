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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base extension of {@link RuntimePerClassExtension} that injects the {@link IdentityHubEndToEndTestContext}
 * when required.
 */
public abstract class IdentityHubEndToEndExtension extends RuntimePerClassExtension {

    private final IdentityHubEndToEndTestContext context;

    protected IdentityHubEndToEndExtension(IdentityHubEndToEndTestContext context) {
        super(context.getRuntime());
        this.context = context;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(IdentityHubEndToEndTestContext.class)) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(IdentityHubEndToEndTestContext.class)) {
            return context;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    /**
     * In-Memory variant of {@link IdentityHubEndToEndExtension}
     */
    public static class InMemory extends IdentityHubEndToEndExtension {

        protected InMemory() {
            super(context());
        }

        protected InMemory(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            super(context(configProvider));
        }

        public static InMemory withConfig(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            return new InMemory(configProvider);
        }

        public static IdentityHubEndToEndTestContext context() {
            return context((it) -> ConfigFactory.empty());
        }

        public static IdentityHubEndToEndTestContext context(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            var configuration = IdentityHubRuntimeConfiguration.Builder.newInstance()
                    .name("identity-hub")
                    .id("identity-hub")
                    .build();
            return context(configuration, () -> configProvider.apply(configuration));
        }

        public static IdentityHubEndToEndTestContext context(IdentityHubRuntimeConfiguration configuration, Supplier<Config> configSupplier) {
            var runtime = new EmbeddedRuntime(
                    "identity-hub",
                    ":dist:bom:identityhub-with-sts-bom"
            ).configurationProvider(configuration::config)
                    .configurationProvider(configSupplier);

            return new IdentityHubEndToEndTestContext(runtime, configuration);
        }

    }

    /**
     * PG variant of {@link IdentityHubEndToEndExtension}
     */
    public static class Postgres extends IdentityHubEndToEndExtension {


        protected Postgres() {
            this((it) -> ConfigFactory.empty());
        }

        protected Postgres(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            super(context(configProvider));
        }

        public static Postgres withConfig(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            return new Postgres(configProvider);
        }
        
        private static IdentityHubEndToEndTestContext context(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {

            var configuration = IdentityHubRuntimeConfiguration.Builder.newInstance()
                    .name("identity-hub")
                    .id("identity-hub")
                    .build();

            return context(configuration, () -> configProvider.apply(configuration));
        }

        private static IdentityHubEndToEndTestContext context(IdentityHubRuntimeConfiguration configuration, Supplier<Config> configSupplier) {

            var runtime = new EmbeddedRuntime(
                    "identityhub-pg",
                    ":dist:bom:identityhub-with-sts-bom",
                    ":dist:bom:identityhub-feature-sql-bom"

            ).configurationProvider(configuration::config)
                    .configurationProvider(configSupplier);

            return new IdentityHubEndToEndTestContext(runtime, configuration);
        }
    }
}
