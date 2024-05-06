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

package org.eclipse.edc.identityhub.accesstoken.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ClaimIsPresentRuleTest {

    @Test
    void checkRule_whenPresent() {
        var claimToken = ClaimToken.Builder.newInstance().claim("foo", "bar").build();
        assertThat(new ClaimIsPresentRule("foo").checkRule(claimToken, null)).isSucceeded();
    }

    @Test
    void checkRule_whenNotPresent() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        assertThat(new ClaimIsPresentRule("foo").checkRule(claimToken, null)).isFailed()
                .detail().isEqualTo("Required claim 'foo' not present on token.");
    }
}