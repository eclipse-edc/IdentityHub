/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.dcp.sql;

import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.SharedPostgresqlEndToEndExtension;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that an IdentityHub and an IssuerService replica can start with {@code edc.sql.schema.autocreate} enabled
 * while other replicas are using the database. A schema statement that needs a lock conflicting with readers (e.g.
 * {@code ALTER TABLE}, which takes an ACCESS EXCLUSIVE lock even when there is nothing to change) would wait for every
 * transaction using the table, while all the other queries queue up behind it.
 */
@PostgresqlIntegrationTest
class SqlSchemaBootstrapEndToEndTest {

    private static final String IDENTITY_HUB = "schema_identityhub";
    private static final String ISSUER = "schema_issuer";

    @Order(0)
    @RegisterExtension
    static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new SharedPostgresqlEndToEndExtension();

    @Order(1)
    @RegisterExtension
    static final BeforeAllCallback SETUP = context -> {
        POSTGRESQL_EXTENSION.createDatabase(IDENTITY_HUB);
        POSTGRESQL_EXTENSION.createDatabase(ISSUER);
    };

    // create the schemas
    @Order(2)
    @RegisterExtension
    static final RuntimeExtension IDENTITY_HUB_EXTENSION = identityHub("identity-hub", () -> POSTGRESQL_EXTENSION.configFor(IDENTITY_HUB));

    @Order(2)
    @RegisterExtension
    static final RuntimeExtension ISSUER_EXTENSION = issuer("issuerservice", () -> POSTGRESQL_EXTENSION.configFor(ISSUER));

    @Test
    void identityHubReplica_shouldStart_whileTransactionReadsAllTables() throws SQLException {
        try (var reader = readAllTables(IDENTITY_HUB)) {
            var replica = identityHub("identity-hub-replica", () -> withLockTimeout(IDENTITY_HUB));
            try {
                assertThatNoException().isThrownBy(() -> replica.beforeAll(null));
            } finally {
                replica.afterAll(null);
            }
            reader.rollback();
        }
    }

    @Test
    void issuerReplica_shouldStart_whileTransactionReadsAllTables() throws SQLException {
        try (var reader = readAllTables(ISSUER)) {
            var replica = issuer("issuerservice-replica", () -> withLockTimeout(ISSUER));
            try {
                assertThatNoException().isThrownBy(() -> replica.beforeAll(null));
            } finally {
                replica.afterAll(null);
            }
            reader.rollback();
        }
    }

    /**
     * Opens a transaction that reads every table of the database, and leaves it open.
     */
    private static Connection readAllTables(String database) throws SQLException {
        var reader = DriverManager.getConnection(POSTGRESQL_EXTENSION.getJdbcUrl(database), POSTGRESQL_EXTENSION.getUsername(), POSTGRESQL_EXTENSION.getPassword());
        var statement = reader.createStatement();
        reader.setAutoCommit(false);
        var tables = new ArrayList<String>();
        try (var resultSet = statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = current_schema()")) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        assertThat(tables).isNotEmpty();
        for (var table : tables) {
            statement.execute("SELECT 1 FROM " + table + " LIMIT 1");
        }
        return reader;
    }

    // the replica's schema statements give up after 1 s instead of waiting for the reader
    private static Config withLockTimeout(String database) {
        return POSTGRESQL_EXTENSION.configFor(database).merge(ConfigFactory.fromMap(Map.of("edc.datasource.default.url",
                POSTGRESQL_EXTENSION.getJdbcUrl(database) + "?options=-c%20lock_timeout%3D1000")));
    }

    private static ComponentRuntimeExtension identityHub(String name, Supplier<Config> database) {
        return ComponentRuntimeExtension.Builder.newInstance()
                .name(name)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(database)
                .build();
    }

    private static ComponentRuntimeExtension issuer(String name, Supplier<Config> database) {
        return ComponentRuntimeExtension.Builder.newInstance()
                .name(name)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(database)
                .build();
    }
}
