/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A {@link PostgresqlEndToEndExtension} that starts one PostgreSQL container per test JVM and shares it between all test
 * classes, instead of starting a new container for every class. The container is not stopped after a class, Testcontainers
 * removes it when the JVM exits.
 * <p>
 * Every test class still gets empty databases: {@link #createDatabase(String)} drops a database of the same name, together
 * with the connections that a previous runtime may have left open.
 */
public class SharedPostgresqlEndToEndExtension extends PostgresqlEndToEndExtension {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.3");

    public SharedPostgresqlEndToEndExtension() {
        super(POSTGRES);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // a container that failed to start, or has stopped, is removed, so that it is started anew instead of breaking
        // every following test class
        if (!POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // keep the container running for the next test class
    }

    @Override
    public void createDatabase(String name) {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("drop database if exists %s with (force)".formatted(name));
            statement.execute("create database %s".formatted(name));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
