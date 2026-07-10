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

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegexScopeToCriterionTransformerTest {

    private final ScopeMappingRegistryImpl registry = new ScopeMappingRegistryImpl();
    private final ScopeToCriterionTransformer fallback = mock();
    private final RegexScopeToCriterionTransformer transformer = new RegexScopeToCriterionTransformer(registry, fallback);

    @Test
    void transformScope_shouldUseRegistry_whenMappingMatches() {
        registry.addMapping("org\\.eclipse\\.custom\\.vc\\.type:(.+):(read|\\*|all)",
                new Criterion("verifiableCredential.credential.type", "contains", "$1"));

        var result = transformer.transformScope("org.eclipse.custom.vc.type:MembershipCredential:read");

        assertThat(result).isSucceeded().satisfies(criteria -> {
            assertThat(criteria).singleElement().satisfies(c -> {
                assertThat(c.getOperandLeft()).isEqualTo("verifiableCredential.credential.type");
                assertThat(c.getOperator()).isEqualTo("contains");
                assertThat(c.getOperandRight()).isEqualTo("MembershipCredential");
            });
        });
        verifyNoInteractions(fallback);
    }

    @Test
    void transformScope_shouldDelegateToFallback_whenNoMappingMatches() {
        registry.addMapping("nomatch:(.+)", new Criterion("type", "contains", "$1"));
        when(fallback.transformScope("org.eclipse.dspace.dcp.vc.type:TestCredential:read"))
                .thenReturn(failure("some failure"));

        var result = transformer.transformScope("org.eclipse.dspace.dcp.vc.type:TestCredential:read");

        assertThat(result).isFailed();
        verify(fallback).transformScope("org.eclipse.dspace.dcp.vc.type:TestCredential:read");
    }

    @Test
    void transformScope_shouldDelegateToFallback_whenRegistryEmpty() {
        when(fallback.transformScope("any:scope:read")).thenReturn(failure("fallback used"));

        transformer.transformScope("any:scope:read");

        verify(fallback).transformScope("any:scope:read");
    }
}
