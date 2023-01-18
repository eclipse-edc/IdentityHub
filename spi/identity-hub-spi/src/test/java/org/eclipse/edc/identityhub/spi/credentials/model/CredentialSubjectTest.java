/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credentials.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CredentialSubjectTest extends AbstractSerDeserTest<CredentialSubject> {

    @Override
    protected Class<CredentialSubject> getClazz() {
        return CredentialSubject.class;
    }

    @Override
    protected CredentialSubject getEntity() {
        return CredentialSubject.Builder.newInstance()
                .id("id-test")
                .claim("foo", "bar")
                .claim("hello", "world")
                .build();
    }

    @Test
    void verifyIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> CredentialSubject.Builder.newInstance().build())
                .withMessageContaining("`id`");
    }

    @Test
    void verifySerializationFlattensClaims() throws JsonProcessingException {
        var subject = CredentialSubject.Builder.newInstance()
                .id("id-test")
                .claim("foo", "bar")
                .build();

        var json = getMapper().writeValueAsString(subject);
        assertNotNull(json);

        assertThat(json)
                .contains("\"id\":\"id-test\"")
                .contains("\"foo\":\"bar\"")
                .doesNotContain("claims");
    }

    @Test
    void verifySerializationIgnoresEmptyClaims() throws JsonProcessingException {
        var subject = CredentialSubject.Builder.newInstance()
                .id("id-test")
                .build();

        var json = getMapper().writeValueAsString(subject);
        assertNotNull(json);

        assertThat(json).isEqualTo("{\"id\":\"id-test\"}");

        var deser = getMapper().readValue(json, getClazz());
        assertThat(deser).usingRecursiveComparison().isEqualTo(subject);
    }
}