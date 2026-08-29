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

package org.eclipse.edc.identityhub.defaults;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class EdcScopeToCriterionTransformerTest {
    private final DiscriminatorMappingRegistryImpl discriminatorMappingRegistry = new DiscriminatorMappingRegistryImpl();
    private final EdcScopeToCriterionTransformer transformer = new EdcScopeToCriterionTransformer(discriminatorMappingRegistry);

    @ParameterizedTest
    @ValueSource(strings = {
            "org.eclipse.dspace.dcp.vc.type:TestCredential:read",
            "org.eclipse.dspace.dcp.vc.type:TestCredential:*",
            "org.eclipse.dspace.dcp.vc.type:TestCredential:all",
            "org.eclipse.dspace.dcp.vc.type:foo:all",
            "org.eclipse.dspace.dcp.vc.type:https://example.com/contexts/v1#TestCredential:read",
            "org.eclipse.dspace.dcp.vc.type:https://example.com/contexts/v1/#TestCredential:read",
    })
    void transform_validScope(String scope) {
        assertThat(transformer.transformScope(scope)).isSucceeded();
    }

    @Test
    void transform_withAlias() {
        discriminatorMappingRegistry.addMapping("SomeFancyCredential", "https://example.com/contexts/v1#TestCredential");
        assertThat(transformer.transformScope("org.eclipse.dspace.dcp.vc.type:SomeFancyCredential:read")).isSucceeded()
                .satisfies(criteria -> {
                    assertThat(criteria).hasSize(2);
                    assertThat(criteria).anyMatch(c -> c.getOperandRight().equals("https://example.com/contexts/v1"));
                    assertThat(criteria).anyMatch(c -> c.getOperandRight().equals("TestCredential"));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalidAlias:TestCredential:read",
            "org.eclipse.dspace.dcp.vc.type:TestCredential:write",
            "org.eclipse.dspace.dcp.vc.type:TestCredential:foo",
            "org.eclipse.edc::foo",
            "org.eclipse.edc:foo",
            "org.eclipse.edc:https://example.com/contexts/v1#:foo",
    })
    void transform_invalidScope(String scope) {
        assertThat(transformer.transformScope(scope)).isFailed();
    }

    // CS-PRES-11: the org.eclipse.dspace.dcp.vc.id alias MUST be supported and selects one credential by its id
    @Test
    @DisplayName("CS-PRES-11: the vc.id alias resolves to an equality criterion on the credential id")
    void transform_idAlias() {
        assertThat(transformer.transformScope("org.eclipse.dspace.dcp.vc.id:8247b87d-8d72-47e1-8128-9ce47e3d829d"))
                .isSucceeded()
                .satisfies(criteria -> {
                    assertThat(criteria).hasSize(1);
                    assertThat(criteria.get(0).getOperandLeft()).isEqualTo("verifiableCredential.credential.id");
                    assertThat(criteria.get(0).getOperator()).isEqualTo("=");
                    assertThat(criteria.get(0).getOperandRight()).isEqualTo("8247b87d-8d72-47e1-8128-9ce47e3d829d");
                });
    }

    // the alias carries no operation part, so everything after the first separator belongs to the id
    @Test
    @DisplayName("CS-PRES-11: an id containing separators is not truncated")
    void transform_idAlias_idWithSeparators() {
        assertThat(transformer.transformScope("org.eclipse.dspace.dcp.vc.id:urn:uuid:8247b87d-8d72-47e1-8128-9ce47e3d829d"))
                .isSucceeded()
                .satisfies(criteria -> assertThat(criteria.get(0).getOperandRight()).isEqualTo("urn:uuid:8247b87d-8d72-47e1-8128-9ce47e3d829d"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "org.eclipse.dspace.dcp.vc.id:",
            "org.eclipse.dspace.dcp.vc.id: ",
    })
    @DisplayName("CS-PRES-11: the vc.id alias without an id is rejected")
    void transform_idAlias_withoutId(String scope) {
        assertThat(transformer.transformScope(scope)).isFailed();
    }
}
