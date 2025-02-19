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

package org.eclipse.edc.issuerservice.issuance.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.issuerservice.issuance.common.JsonNavigator.navigateProperty;

class JsonNavigatorTest {

    @Test
    void verify_string_property() {
        var result = navigateProperty(new String[]{ "foo" }, Map.of("foo", "value"), true);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("value");
    }

    @Test
    void verify_nested_string_property() {
        var result = navigateProperty(new String[]{ "foo", "bar" }, Map.of("foo", Map.of("bar", "value")), true);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("value");
    }

    @Test
    void verify_nested_int_property() {
        var result = navigateProperty(new String[]{ "foo", "bar" }, Map.of("foo", Map.of("bar", 1)), true);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(1);
    }

    @Test
    void verify_nested_complex_property() {
        var result = navigateProperty(new String[]{ "foo", "bar" }, Map.of("foo", Map.of("bar", new Object())), true);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();
    }

    @Test
    void verify_not_required_property() {
        var result = navigateProperty(new String[]{ "notthere" }, Map.of("foo", "value"), false);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNull();
    }

    @Test
    void verify_required_missing_property() {
        var result = navigateProperty(new String[]{ "notthere" }, Map.of("foo", "value"), true);
        assertThat(result.failed()).isTrue();
    }
}