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

package org.eclipse.edc.issuerservice.issuance.mapping;

import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class IssuanceClaimsMapperImplTest {


    private final IssuanceClaimsMapper mapper = new IssuanceClaimsMapperImpl();


    @Test
    void apply() {
        var mappingDefinition = new MappingDefinition("person.name", "credentialSubject.name", true);
        var result = mapper.apply(mappingDefinition, Map.of("person", Map.of("name", "Alice")));
        assertThat(result).isSucceeded().satisfies(claims -> {
            assertThat(claims).containsEntry("credentialSubject", Map.of("name", "Alice"));
        });
    }

    @Test
    void apply_whenRequired() {
        var mappingDefinition = new MappingDefinition("person.surname", "credentialSubject.name", true);
        var result = mapper.apply(mappingDefinition, Map.of("person", Map.of("name", "Alice")));
        assertThat(result).isFailed();
    }

    @Test
    void apply_whenNotRequired() {
        var mappingDefinition = new MappingDefinition("person.surname", "credentialSubject.name", false);
        var result = mapper.apply(mappingDefinition, Map.of("person", Map.of("name", "Alice")));
        assertThat(result).isSucceeded().satisfies(claims -> {
            assertThat(claims).isEmpty();
        });
    }

    @Test
    void apply_whenMultipleMappings() {
        var mappingDefinition1 = new MappingDefinition("person.surname", "credentialSubject.name", true);
        var mappingDefinition2 = new MappingDefinition("credential.id", "credentialSubject.id", true);
        var mappingDefinition3 = new MappingDefinition("time", "issuanceDate", true);

        Map<String, Object> mappings = Map.of("person", Map.of("surname", "Allison", "firstname", "Trudy"),
                "credential", Map.of("id", "test cred id"),
                "time", Instant.now().toString());

        var result1 = mapper.apply(List.of(mappingDefinition1, mappingDefinition2, mappingDefinition3), mappings);
        assertThat(result1).isSucceeded().satisfies(claims -> {
            assertThat(claims).containsEntry("credentialSubject", Map.of("name", "Allison", "id", "test cred id"));
            assertThat(claims).hasEntrySatisfying("issuanceDate", v -> assertThat(v).isNotNull());
        });
    }
}
