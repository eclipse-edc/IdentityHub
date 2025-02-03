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

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.identityhub.tests.fixtures.PostgresSqlService.jdbcUrl;
import static org.eclipse.edc.util.io.Ports.getFreePort;

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

        protected InMemory() {
            super(context());
        }

        public static IssuerServiceEndToEndTestContext context() {
            var configuration = IssuerServiceRuntimeConfiguration.Builder.newInstance()
                    .name("issuerservice")
                    .id("issuerservice")
                    .build();

            var runtime = new EmbeddedRuntime(
                    "issuerservice",
                    configuration.config(),
                    ":dist:bom:issuerservice-bom"
            );

            return new IssuerServiceEndToEndTestContext(runtime, configuration);
        }

    }

    /**
     * PG variant of {@link IssuerServiceEndToEndExtension}
     */
    public static class Postgres extends IssuerServiceEndToEndExtension {

        private static final String DB_NAME = "issuerservice";
        private static final Integer DB_PORT = getFreePort();
        private final PostgresSqlService postgresSqlService;

        protected Postgres() {
            super(context(DB_NAME, DB_PORT));
            postgresSqlService = new PostgresSqlService(DB_NAME, DB_PORT);

        }

        public static IssuerServiceEndToEndTestContext context(String dbName, Integer port) {

            var configuration = IssuerServiceRuntimeConfiguration.Builder.newInstance()
                    .name("issuerservice")
                    .id("issuerservice")
                    .build();

            var cfg = new HashMap<>(configuration.config());
            cfg.putAll(postgresqlConfiguration(dbName, port));

            var runtime = new EmbeddedRuntime(
                    "issuerservice-pg",
                    cfg,
                    ":dist:bom:issuerservice-bom",
                    ":dist:bom:issuerservice-feature-sql-bom"

            );

            return new IssuerServiceEndToEndTestContext(runtime, configuration);
        }

        private static Map<String, String> postgresqlConfiguration(String dbName, Integer port) {
            var jdbcUrl = jdbcUrl(dbName, port);
            return new HashMap<>() {
                {
                    put("edc.datasource.default.url", jdbcUrl);
                    put("edc.datasource.default.user", PostgresqlEndToEndInstance.USER);
                    put("edc.datasource.default.password", PostgresqlEndToEndInstance.PASSWORD);
                }
            };
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
