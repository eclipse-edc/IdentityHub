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

package org.eclipse.edc.issuerservice.issuance.attestations.presentation;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationAttestationSourceTest {

    @Test
    void verify_required() {
        var source = new PresentationAttestationSource("test", "testOut", true);
        var claims = Map.of("test", ClaimToken.Builder.newInstance().build());
        var result = source.execute(new TestAttestationContext("participant", claims));
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).containsKey("testOut");
    }

    @Test
    void verify_optional() {
        var source = new PresentationAttestationSource("test", "testOut", false);
        assertThat(source.execute(new TestAttestationContext("participant", Map.of())).succeeded()).isTrue();
    }

    @Test
    void verify_notPresent() {
        var source = new PresentationAttestationSource("test", "testOut", true);
        assertThat(source.execute(new TestAttestationContext("participant", Map.of())).failed()).isTrue();
    }
}