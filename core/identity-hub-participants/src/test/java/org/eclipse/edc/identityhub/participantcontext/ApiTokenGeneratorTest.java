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

package org.eclipse.edc.identityhub.participantcontext;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTokenGeneratorTest {

    @Test
    void verifyGenerate_defaultLength() {
        var token = new ApiTokenGenerator().generate("test-principal");
        assertThat(token).contains(".");
        var split = token.split("\\.");
        assertThat(split).hasSize(2);
        assertThat(Base64.getDecoder().decode(split[0])).isEqualTo("test-principal".getBytes());
        assertThat(Base64.getDecoder().decode(split[1])).hasSize(64);
    }

    @Test
    void verifyGenerate_specificLength() {
        var token = new ApiTokenGenerator(128).generate("test-principal");
        assertThat(token).contains(".");
        var split = token.split("\\.");
        assertThat(split).hasSize(2);
        assertThat(Base64.getDecoder().decode(split[0])).isEqualTo("test-principal".getBytes());
        assertThat(Base64.getDecoder().decode(split[1])).hasSize(128);
    }
}