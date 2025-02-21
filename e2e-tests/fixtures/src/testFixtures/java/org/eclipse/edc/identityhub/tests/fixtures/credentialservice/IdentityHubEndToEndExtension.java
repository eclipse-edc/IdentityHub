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

import org.eclipse.edc.identityhub.tests.fixtures.PostgresSqlService;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.eclipse.edc.identityhub.tests.fixtures.PostgresSqlService.jdbcUrl;
import static org.eclipse.edc.util.io.Ports.getFreePort;

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

        private static final String DB_NAME = "runtime";
        private static final Integer DB_PORT = getFreePort();
        private final PostgresSqlService postgresSqlService;

        protected Postgres() {
            this((it) -> ConfigFactory.empty());
        }

        protected Postgres(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            super(context(DB_NAME, DB_PORT, configProvider));
            postgresSqlService = new PostgresSqlService(DB_NAME, DB_PORT);
        }

        public static Postgres withConfig(Function<IdentityHubRuntimeConfiguration, Config> configProvider) {
            return new Postgres(configProvider);
        }

        public static IdentityHubEndToEndTestContext context(String dbName, Integer port) {
            return context(dbName, port, (it) -> ConfigFactory.empty());
        }

        private static IdentityHubEndToEndTestContext context(String dbName, Integer port, Function<IdentityHubRuntimeConfiguration, Config> configProvider) {

            var configuration = IdentityHubRuntimeConfiguration.Builder.newInstance()
                    .name("identity-hub")
                    .id("identity-hub")
                    .build();

            return context(configuration, () -> postgresqlConfiguration(dbName, port)
                    .merge(configProvider.apply(configuration)));
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

        private static Config postgresqlConfiguration(String dbName, Integer port) {
            var jdbcUrl = jdbcUrl(dbName, port);
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.datasource.default.url", jdbcUrl);
                    put("edc.datasource.default.user", PostgresqlEndToEndInstance.USER);
                    put("edc.datasource.default.password", PostgresqlEndToEndInstance.PASSWORD);
                }
            });
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) {
            postgresSqlService.start();
            super.beforeAll(extensionContext);
        }

        @Override
        public void afterAll(ExtensionContext extensionContext) {
            super.afterAll(extensionContext);
            postgresSqlService.stop();
        }
    }
}
