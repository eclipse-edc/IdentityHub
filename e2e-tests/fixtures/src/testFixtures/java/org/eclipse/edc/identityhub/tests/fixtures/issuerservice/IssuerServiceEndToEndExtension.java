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

package org.eclipse.edc.identityhub.tests.fixtures.issuerservice;

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
 * Base extension of {@link RuntimePerClassExtension} that injects the {@link IssuerServiceEndToEndTestContext}
 * when required.
 */
public abstract class IssuerServiceEndToEndExtension extends RuntimePerClassExtension {

    private final IssuerServiceEndToEndTestContext context;

    protected IssuerServiceEndToEndExtension(IssuerServiceEndToEndTestContext context) {
        super(context.getRuntime());
        this.context = context;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(IssuerServiceEndToEndTestContext.class)) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(IssuerServiceEndToEndTestContext.class)) {
            return context;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    /**
     * In-Memory variant of {@link IssuerServiceEndToEndExtension}
     */
    public static class InMemory extends IssuerServiceEndToEndExtension {

        public InMemory() {
            this((it) -> ConfigFactory.empty());
        }

        protected InMemory(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            super(context(configProvider));
        }

        public static InMemory withConfig(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            return new InMemory(configProvider);
        }

        public static IssuerServiceEndToEndTestContext context() {
            return context((it) -> ConfigFactory.empty());
        }

        public static IssuerServiceEndToEndTestContext context(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            var configuration = IssuerServiceRuntimeConfiguration.Builder.newInstance()
                    .name("issuerservice")
                    .id("issuerservice")
                    .build();

            return context(configuration, () -> configProvider.apply(configuration));
        }

        public static IssuerServiceEndToEndTestContext context(IssuerServiceRuntimeConfiguration configuration, Supplier<Config> configSupplier) {
            var runtime = new EmbeddedRuntime(
                    "issuerservice",
                    ":dist:bom:issuerservice-bom"
            ).configurationProvider(configuration::config).configurationProvider(configSupplier);

            return new IssuerServiceEndToEndTestContext(runtime, configuration);
        }

    }

    /**
     * PG variant of {@link IssuerServiceEndToEndExtension}
     */
    public static class Postgres extends IssuerServiceEndToEndExtension {


        public Postgres() {
            this((it) -> ConfigFactory.empty());
        }

        protected Postgres(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            super(context(configProvider));
        }

        public static Postgres withConfig(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            return new Postgres(configProvider);
        }

        private static IssuerServiceEndToEndTestContext context(Function<IssuerServiceRuntimeConfiguration, Config> configProvider) {
            var configuration = IssuerServiceRuntimeConfiguration.Builder.newInstance()
                    .name("issuerservice")
                    .id("issuerservice")
                    .build();

            return context(configuration, () -> configProvider.apply(configuration));
        }

        private static IssuerServiceEndToEndTestContext context(IssuerServiceRuntimeConfiguration configuration, Supplier<Config> configSupplier) {

            var runtime = new EmbeddedRuntime(
                    "issuerservice-pg",
                    ":dist:bom:issuerservice-bom",
                    ":dist:bom:issuerservice-feature-sql-bom"

            ).configurationProvider(configuration::config)
                    .configurationProvider(configSupplier);

            return new IssuerServiceEndToEndTestContext(runtime, configuration);
        }


    }
}
