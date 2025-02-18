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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStoreTestBase;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlCredentialDefinitionStoreTest extends CredentialDefinitionStoreTestBase {

    private final CredentialDefinitionStoreStatements statements = new PostgresDialectStatements();
    private SqlCredentialDefinitionStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlCredentialDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, Clock.systemUTC());

        var schema = TestUtils.getResourceFileContentAsString("credential-definition-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getCredentialDefinitionTable() + " CASCADE");
    }

    // queries that introspect JSON are not supported in the in-mem variant
    @Test
    void byJsonSchema_introspectingJson() {
        var def = createCredentialDefinitionBuilder("id", "Membership")
                .jsonSchemaUrl(null)
                .jsonSchema("""
                        {
                          "$id": "https://example.com/person.schema.json",
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "title": "Person",
                          "type": "object",
                          "properties": {
                            "firstName": {
                              "type": "string",
                              "description": "The person's first name."
                            },
                            "age": {
                              "description": "Age in years which must be equal to or greater than zero.",
                              "type": "integer",
                              "minimum": 0
                            }
                          }
                        }
                        """)
                .build();

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("jsonSchema.title", "=", "Person"))
                .build();

        getStore().create(def);
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).hasSize(1)
                .allSatisfy(cd -> Assertions.assertThat(cd).usingRecursiveComparison().isEqualTo(def));
    }

    @Test
    void byRuleConfiguration() {
        var def1 = createCredentialDefinitionBuilder("id1", "Membership")
                .rule(new CredentialRuleDefinition("rule-type-1", Map.of("ruleConfigKey", "ruleConfigValue")))
                .attestations(Set.of("att1", "att2"))
                .build();
        var def2 = createCredentialDefinitionBuilder("id2", "Iso9001Cert")
                .rule(new CredentialRuleDefinition("rule-type-2", Map.of("ruleConfigKey", "ruleConfigValue", "ruleConfigKey2", "ruleConfigValue2")))
                .rule(new CredentialRuleDefinition("rule-type-2", Map.of("anotherRuleConfigKey", "anotherRuleConfigValue")))
                .attestations(Set.of("att2", "att3"))
                .build();

        var r = getStore().create(def1).compose(v -> getStore().create(def2));
        assertThat(r).isSucceeded();

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("rules.configuration.ruleConfigKey", "=", "ruleConfigValue"))
                .build();

        var result = getStore().query(query);
        assertThat(result).isSucceeded();

        Assertions.assertThat(result.getContent())
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(def1, def2);
    }

    @Override
    protected CredentialDefinitionStore getStore() {
        return store;
    }
}