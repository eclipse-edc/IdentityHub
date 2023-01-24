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
import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CredentialTest extends AbstractSerDeserTest<Credential> {

    @Override
    protected Class<Credential> getClazz() {
        return Credential.class;
    }

    @Override
    protected Credential getEntity() {
        return VerifiableCredentialTestUtil.generateCredential();
    }

    @Test
    void verifyNullFieldNotSerialized() throws JsonProcessingException {
        var vc = VerifiableCredentialTestUtil.generateCredential();

        var json = getMapper().writeValueAsString(vc);

        Assertions.assertThat(json)
                .doesNotContain("credentialStatus")
                .doesNotContain("expirationDate");
    }

    @Test
    void verifyAtLeastOneContext() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> Credential.Builder.newInstance().build())
                .withMessageContaining("context");
    }

    @Test
    void verifyIssuerMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Credential.Builder.newInstance()
                        .context("context")
                        .build())
                .withMessageContaining("`issuer`");
    }

    @Test
    void verifyIdMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Credential.Builder.newInstance()
                        .context("context")
                        .issuer("issuer")
                        .build())
                .withMessageContaining("`id`");
    }

    @Test
    void verifyCredentialSubjectMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Credential.Builder.newInstance()
                        .context("context")
                        .issuer("issuer")
                        .id("id")
                        .build())
                .withMessageContaining("`credentialSubject`");
    }

    @Test
    void verifyIssuanceMandatoryMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Credential.Builder.newInstance()
                        .context("context")
                        .issuer("issuer")
                        .id("id")
                        .credentialSubject(CredentialSubject.Builder.newInstance().id("test").build())
                        .build())
                .withMessageContaining("`issuanceDate`");
    }
}
