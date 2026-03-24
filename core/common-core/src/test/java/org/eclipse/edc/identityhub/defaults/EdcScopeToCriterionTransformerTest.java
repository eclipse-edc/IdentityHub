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
                    assertThat(criteria.stream().anyMatch(c -> c.getOperandRight().equals("https://example.com/contexts/v1")));
                    assertThat(criteria.stream().anyMatch(c -> c.getOperandRight().equals("TestCredential")));
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
}