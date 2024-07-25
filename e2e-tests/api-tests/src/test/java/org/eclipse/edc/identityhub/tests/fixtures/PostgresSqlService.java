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

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Wrapper for {@link PostgreSQLContainer}
 */
public class PostgresSqlService {

    private static final String POSTGRES_IMAGE_NAME = "postgres:16.2";

    private final PostgreSQLContainer<?> postgreSqlContainer;
    private final String dbName;
    private final Integer hostPort;

    public PostgresSqlService(Integer port) {
        this("runtime", port);
    }

    public PostgresSqlService(String dbName, Integer port) {
        this.hostPort = port;
        this.dbName = dbName;
        var portBinding = new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(5432));
        postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
                .withLabel("runtime", dbName)
                .withExposedPorts(5432)
                .withUsername(PostgresqlEndToEndInstance.USER)
                .withPassword(PostgresqlEndToEndInstance.PASSWORD)
                .withDatabaseName(dbName)
                .withCreateContainerCmdModifier(e -> e.withHostConfig(new HostConfig().withPortBindings(portBinding)));
    }

    public static String baseJdbcUrl(Integer hostPort) {
        return format("jdbc:postgresql://%s:%s/", "localhost", hostPort);
    }

    public static String jdbcUrl(String name, Integer hostPort) {
        return baseJdbcUrl(hostPort) + name;
    }
    
    public void start() {
        postgreSqlContainer.start();
        postgreSqlContainer.waitingFor(Wait.forHealthcheck());
        createDatabase();
    }

    public void stop() {
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
    }

    public String jdbcUrl() {
        return jdbcUrl(dbName, hostPort);
    }

    private void createDatabase() {
        var postgres = new PostgresqlLocalInstance(PostgresqlEndToEndInstance.USER, PostgresqlEndToEndInstance.PASSWORD, baseJdbcUrl(hostPort), dbName);
        postgres.createDatabase();

        var extensionsFolder = TestUtils.findBuildRoot().toPath().resolve("extensions");
        var scripts = Stream.of(
                        "store/sql/identity-hub-credentials-store-sql",
                        "store/sql/identity-hub-did-store-sql",
                        "store/sql/identity-hub-keypair-store-sql",
                        "store/sql/identity-hub-participantcontext-store-sql"
                )
                .map(extensionsFolder::resolve)
                .map(it -> it.resolve("docs"))
                .map(it -> it.resolve("schema.sql"))
                .toList();

        try (var connection = postgres.getConnection(dbName)) {
            for (var script : scripts) {
                var sql = Files.readString(script);

                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (Exception exception) {
                    throw new EdcPersistenceException(exception.getMessage(), exception);
                }
            }
        } catch (SQLException | IOException e) {
            throw new EdcPersistenceException(e);
        }
    }
}
