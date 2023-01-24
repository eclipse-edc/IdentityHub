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

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateProof;

class VerifiableCredentialTest extends AbstractSerDeserTest<VerifiableCredential> {

    private static final String VC_FILE = "vc.json";

    @Override
    protected Class<VerifiableCredential> getClazz() {
        return VerifiableCredential.class;
    }

    @Override
    protected VerifiableCredential getEntity() {
        try (var is = getClass().getClassLoader().getResourceAsStream(VC_FILE)) {
            Objects.requireNonNull(is, "Failed to open file: " + VC_FILE);
            return getMapper().readValue(is, VerifiableCredential.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void verifyVcIsValid() {
        var credential = defaultCredential()
                .type("VerifiableCredential")
                .context("https://www.w3.org/2018/credentials/v1")
                .build();

        var vc = new VerifiableCredential(credential, generateProof());

        assertThat(vc.isValid()).isTrue();
    }

    @Test
    void verifyVcNotValidIfMissingDefaultContext() {
        var credential = defaultCredential()
                .context("unknown-context")
                .type("VerifiableCredential")
                .build();

        var vc = new VerifiableCredential(credential, generateProof());

        assertThat(vc.isValid()).isFalse();
    }

    @Test
    void verifyVcNotValidIfMissingDefaultType() {
        var credential = defaultCredential()
                .context("https://www.w3.org/2018/credentials/v1")
                .build();

        var vc = new VerifiableCredential(credential, generateProof());

        assertThat(vc.isValid()).isFalse();
    }

    private static Credential.Builder defaultCredential() {
        return Credential.Builder.newInstance()
                .id("test")
                .issuer("issuer")
                .issuanceDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test").build());
    }
}