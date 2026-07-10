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

import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeMappingRegistryImplTest {

    private final ScopeMappingRegistryImpl registry = new ScopeMappingRegistryImpl();

    @Test
    void map_shouldSubstituteCaptureGroupInRightOperand() {
        registry.addMapping("org\\.eclipse\\.custom\\.vc\\.type:(.+):(read|\\*|all)",
                new Criterion("verifiableCredential.credential.type", "contains", "$1"));

        var criteria = registry.map("org.eclipse.custom.vc.type:MembershipCredential:read");

        assertThat(criteria).singleElement().satisfies(c -> {
            assertThat(c.getOperandLeft()).isEqualTo("verifiableCredential.credential.type");
            assertThat(c.getOperator()).isEqualTo("contains");
            assertThat(c.getOperandRight()).isEqualTo("MembershipCredential");
        });
    }

    @Test
    void map_shouldSubstituteCaptureGroupInLeftOperand() {
        registry.addMapping("(.+):(.+):read",
                new Criterion("$1", "contains", "$2"));

        var criteria = registry.map("someLeft:someRight:read");

        assertThat(criteria).singleElement().satisfies(c -> {
            assertThat(c.getOperandLeft()).isEqualTo("someLeft");
            assertThat(c.getOperandRight()).isEqualTo("someRight");
        });
    }

    @Test
    void map_shouldSupportBracedGroupReference() {
        registry.addMapping("type:(.+)", new Criterion("type", "contains", "${1}Suffix"));

        var criteria = registry.map("type:Membership");

        assertThat(criteria).singleElement()
                .satisfies(c -> assertThat(c.getOperandRight()).isEqualTo("MembershipSuffix"));
    }

    @Test
    void map_shouldAccumulateAllMatchingMappings() {
        registry.addMapping("vc:(.+):read", new Criterion("verifiableCredential.credential.type", "contains", "$1"));
        registry.addMapping("vc:(.+):read", new Criterion("verifiableCredential.credential.@context", "contains", "https://example.com"));

        var criteria = registry.map("vc:MembershipCredential:read");

        assertThat(criteria).hasSize(2);
        assertThat(criteria).anyMatch(c -> c.getOperandRight().equals("MembershipCredential"));
        assertThat(criteria).anyMatch(c -> c.getOperandRight().equals("https://example.com"));
    }

    @Test
    void map_shouldReturnEmptyList_whenNoMappingMatches() {
        registry.addMapping("vc:(.+):read", new Criterion("verifiableCredential.credential.type", "contains", "$1"));

        assertThat(registry.map("something:completely:different")).isEmpty();
    }

    @Test
    void map_shouldReturnEmptyList_whenNoMappingsRegistered() {
        assertThat(registry.map("vc:MembershipCredential:read")).isEmpty();
    }

    @Test
    void map_shouldReturnEmptyList_whenScopeIsNull() {
        registry.addMapping("vc:(.+):read", new Criterion("verifiableCredential.credential.type", "contains", "$1"));

        assertThat(registry.map(null)).isEmpty();
    }

    @Test
    void map_shouldOnlyMatchFullString() {
        registry.addMapping("vc:(.+)", new Criterion("type", "contains", "$1"));

        // 'matches()' requires the whole string to match, so a leading prefix means no match
        assertThat(registry.map("prefix-vc:Membership")).isEmpty();
    }

    @Test
    void map_shouldLeaveLiteral_whenGroupReferenceIsOutOfRange() {
        registry.addMapping("vc:(.+)", new Criterion("type", "contains", "$2"));

        assertThat(registry.map("vc:Membership")).singleElement()
                .satisfies(c -> assertThat(c.getOperandRight()).isEqualTo("$2"));
    }

    @Test
    void map_shouldLeaveLiteral_whenGroupReferenceOverflowsInt() {
        registry.addMapping("vc:(.+)", new Criterion("type", "contains", "$99999999999"));

        assertThat(registry.map("vc:Membership")).singleElement()
                .satisfies(c -> assertThat(c.getOperandRight()).isEqualTo("$99999999999"));
    }

    @Test
    void addMapping_shouldThrow_whenRegexIsInvalid() {
        assertThatThrownBy(() -> registry.addMapping("vc:(.+", new Criterion("type", "contains", "$1")))
                .isInstanceOf(PatternSyntaxException.class);
    }
}
