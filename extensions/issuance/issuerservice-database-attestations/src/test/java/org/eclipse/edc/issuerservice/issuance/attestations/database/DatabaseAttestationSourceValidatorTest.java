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

package org.eclipse.edc.issuerservice.issuance.attestations.database;

import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSourceValidator;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


class DatabaseAttestationSourceValidatorTest {

    private final DatabaseAttestationSourceValidator validator = new DatabaseAttestationSourceValidator();

    @Test
    void validate_success() {
        Map<String, Object> configuration = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "tableName", "membership_attestations",
                "dataSourceName", "barbaz");

        var definition = AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();

        assertThat(validator.validate(definition)).isSucceeded();
    }


    @Test
    void validate_missingDataSourceName_shouldFail() {
        Map<String, Object> configuration = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "tableName", "membership_attestations");

        var definition = AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();

        assertThat(validator.validate(definition)).isFailed().detail().contains("dataSourceName");
    }

    @Test
    void validate_missingTableName_shouldFail() {
        Map<String, Object> configuration = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "dataSourceName", "foobar");
        var definition = AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();

        assertThat(validator.validate(definition)).isFailed().detail().contains("tableName");
    }

}