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

package org.eclipse.edc.issuerservice.issuance.rules.expression;

import org.eclipse.edc.issuerservice.issuance.rules.expression.ExpressionCredentialRule.Operator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionCredentialRuleTest {

    @Test
    void verify_equal_boolean() {
        var rule = new ExpressionCredentialRule("onboarding.active", Operator.EQ, true);
        assertThat(rule.evaluate(() -> Map.of("onboarding", Map.of("active", true))).succeeded()).isTrue();
    }

    @Test
    void verify_equal_string() {
        var rule = new ExpressionCredentialRule("onboarding.active", Operator.EQ, "true");
        assertThat(rule.evaluate(() -> Map.of("onboarding", Map.of("active", "true"))).succeeded()).isTrue();
    }

    @Test
    void verify_equal_mixed_numerics() {
        var rule = new ExpressionCredentialRule("onboarding.active", Operator.EQ, 1L);
        assertThat(rule.evaluate(() -> Map.of("onboarding", Map.of("active", 1))).succeeded()).isTrue();
    }

    @Test
    void verify_not_equal() {
        var rule = new ExpressionCredentialRule("onboarding.active", Operator.EQ, false);
        assertThat(rule.evaluate(() -> Map.of("onboarding", Map.of("active", true))).succeeded()).isFalse();
    }

    @Test
    void verify_gt() {
        var rule = new ExpressionCredentialRule("value", Operator.GT, 1);
        assertThat(rule.evaluate(() -> Map.of("value", 2)).succeeded()).isTrue();
    }


    @Test
    void verify_geq() {
        var rule = new ExpressionCredentialRule("value", Operator.GEQ, 2);
        assertThat(rule.evaluate(() -> Map.of("value", 2)).succeeded()).isTrue();
    }

    @Test
    void verify_lt() {
        var rule = new ExpressionCredentialRule("value", Operator.LT, 1);
        assertThat(rule.evaluate(() -> Map.of("value", 0)).succeeded()).isTrue();
    }

    @Test
    void verify_let() {
        var rule = new ExpressionCredentialRule("value", Operator.LEQ, 1);
        assertThat(rule.evaluate(() -> Map.of("value", 1)).succeeded()).isTrue();
    }


}