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

        public static IdentityHubEndToEndTestContext context() {
            var configuration = IdentityHubRuntimeConfiguration.Builder.newInstance()
                    .name("identity-hub")
                    .id("identity-hub")
                    .build();

            var runtime = new EmbeddedRuntime(
                    "identity-hub",
                    configuration.config(),
                    ":launcher:identityhub"
            );

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
            super(context(DB_NAME, DB_PORT));
            postgresSqlService = new PostgresSqlService(DB_NAME, DB_PORT);

        }

        public static IdentityHubEndToEndTestContext context(String dbName, Integer port) {

            var configuration = IdentityHubRuntimeConfiguration.Builder.newInstance()
                    .name("identity-hub")
                    .id("identity-hub")
                    .build();

            var cfg = new HashMap<>(configuration.config());
            cfg.putAll(postgresqlConfiguration(dbName, port));

            var runtime = new EmbeddedRuntime(
                    "control-plane",
                    cfg,
                    ":launcher:identityhub",
                    ":extensions:store:sql:identity-hub-credentials-store-sql",
                    ":extensions:store:sql:identity-hub-did-store-sql",
                    ":extensions:store:sql:identity-hub-keypair-store-sql",
                    ":extensions:store:sql:identity-hub-participantcontext-store-sql"

            );

            return new IdentityHubEndToEndTestContext(runtime, configuration);
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
