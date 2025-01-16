/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredential.validation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class VerifiableCredentialManifestValidatorTest {

    private final VerifiableCredentialManifestValidator validator = new VerifiableCredentialManifestValidator();

    @Test
    void validManifest_shouldPassValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, VerifiableCredential.Builder.newInstance()
                        .type("type")
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .id("id")
                                .claim("key", "value")
                                .build())
                        .issuer(new Issuer("issuer"))
                        .issuanceDate(Instant.now())
                        .build()))
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_missingVerifiableCredentialContainer_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().withFailMessage("VerifiableCredentialContainer was null");
    }

    @Test
    void validate_missingParticipantId_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().withFailMessage("Participant id was null");
    }

    @Test
    void validate_missingVerifiableCredential_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, null))
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().withFailMessage("VerifiableCredential was null");
    }

    @Test
    void validate_nullManifest_shouldFailValidation() {
        var result = validator.validate(null);

        assertThat(result).isFailed().withFailMessage("Input was null");
    }

}