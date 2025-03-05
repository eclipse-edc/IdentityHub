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

import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource;
import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DatabaseAttestationSourceFactoryTest {

    private final DatabaseAttestationSourceFactory factory = new DatabaseAttestationSourceFactory(mock(), mock(), mock());

    @Test
    void createSource_whenSucceeds() {
        Map<String, Object> configuration = Map.of("dataSourceName", "test-datasource",
                "tableName", "foobar-table");
        var definition = AttestationDefinition.Builder.newInstance().id("123")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();
        var source = factory.createSource(definition);
        assertThat(source).isInstanceOf(DatabaseAttestationSource.class);
        assertThat((DatabaseAttestationSource) source).extracting(DatabaseAttestationSource::isRequired).isEqualTo(false);

    }

    @Test
    void createSource_whenRequiredSucceeds() {

        Map<String, Object> configuration = Map.of("dataSourceName", "test-datasource",
                "required", true,
                "tableName", "foobar-table");
        var definition = AttestationDefinition.Builder.newInstance().id("123")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();
        var source = factory.createSource(definition);
        assertThat(source).isInstanceOf(DatabaseAttestationSource.class);
        assertThat((DatabaseAttestationSource) source).extracting(DatabaseAttestationSource::isRequired).isEqualTo(true);
    }
}