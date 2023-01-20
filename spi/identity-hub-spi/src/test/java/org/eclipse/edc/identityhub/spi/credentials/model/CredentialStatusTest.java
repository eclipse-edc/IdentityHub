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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CredentialStatusTest extends AbstractSerDeserTest<CredentialStatus> {

    @Override
    protected Class<CredentialStatus> getClazz() {
        return CredentialStatus.class;
    }

    @Override
    protected CredentialStatus getEntity() {
        return CredentialStatus.Builder.newInstance()
                .type("type")
                .id("id")
                .build();
    }

    @Test
    void verifyIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> CredentialStatus.Builder.newInstance().type("type").build())
                .withMessageContaining("`id`");
    }

    @Test
    void verifyTypeMandatory() {
        assertThatNullPointerException().isThrownBy(() -> CredentialStatus.Builder.newInstance().id("id").build())
                .withMessageContaining("`type`");
    }
}