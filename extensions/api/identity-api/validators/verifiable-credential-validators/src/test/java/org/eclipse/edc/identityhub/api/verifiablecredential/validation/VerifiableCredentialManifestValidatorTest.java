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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.validation.CredentialFormatValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class VerifiableCredentialManifestValidatorTest {

    private static final String VCDM11_JWT = unsignedJwt("{\"vc\": {\"@context\": [\"%s\"]}}".formatted(W3C_CREDENTIALS_URL));
    private static final String VCDM20_JWT = unsignedJwt("{\"@context\": [\"%s\"], \"type\": [\"VerifiableCredential\"]}".formatted(VC_PREFIX_V2));
    private static final String VCDM11_LD = "{\"@context\": [\"%s\"], \"type\": [\"VerifiableCredential\"]}".formatted(W3C_CREDENTIALS_URL);
    private static final String VCDM20_LD = "{\"@context\": \"%s\", \"type\": [\"VerifiableCredential\"]}".formatted(VC_PREFIX_V2);

    private final VerifiableCredentialManifestValidator validator = new VerifiableCredentialManifestValidator(new CredentialFormatValidator());

    private static Stream<Arguments> matchingFormats() {
        return Stream.of(
                Arguments.of(VCDM11_JWT, CredentialFormat.VC1_0_JWT),
                Arguments.of(VCDM20_JWT, CredentialFormat.VC2_0_JOSE),
                Arguments.of(VCDM11_LD, CredentialFormat.VC1_0_LD)
        );
    }

    private static Stream<Arguments> mismatchingFormats() {
        return Stream.of(
                Arguments.of(VCDM20_JWT, CredentialFormat.VC1_0_JWT),
                Arguments.of(VCDM11_LD, CredentialFormat.VC1_0_JWT),
                Arguments.of(VCDM11_JWT, CredentialFormat.VC2_0_JOSE),
                Arguments.of(unsignedJwt("{\"type\": [\"VerifiableCredential\"]}"), CredentialFormat.VC2_0_JOSE),
                Arguments.of(VCDM20_LD, CredentialFormat.VC1_0_LD),
                Arguments.of(VCDM11_JWT, CredentialFormat.VC1_0_LD),
                Arguments.of("rawVc", CredentialFormat.VC1_0_JWT)
        );
    }

    @ParameterizedTest
    @MethodSource("matchingFormats")
    void validManifest_shouldPassValidation(String rawVc, CredentialFormat format) {
        var result = validator.validate(manifest(rawVc, format));

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @MethodSource("mismatchingFormats")
    void validate_credentialDoesNotMatchFormat_shouldFailValidation(String rawVc, CredentialFormat format) {
        var result = validator.validate(manifest(rawVc, format));

        assertThat(result).isFailed().messages().singleElement().asString().contains("'%s' credential must".formatted(format));
    }

    @ParameterizedTest
    @EnumSource(value = CredentialFormat.class, names = { "VC2_0_SD_JWT", "VC2_0_COSE" })
    void validate_formatCannotBePresented_shouldFailValidation(CredentialFormat format) {
        var result = validator.validate(manifest(VCDM20_JWT, format));

        assertThat(result).isFailed().messages().containsOnly("Credentials in format '%s' cannot be presented".formatted(format));
    }

    @Test
    void validate_missingFormat_shouldFailValidation() {
        var result = validator.validate(manifest(VCDM11_JWT, null));

        assertThat(result).isFailed().messages().containsOnly("The credential format is missing");
    }

    @Test
    void validate_missingVerifiableCredentialContainer_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().messages().containsOnly("VerifiableCredentialContainer was null");
    }

    @Test
    void validate_missingParticipantId_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().messages().containsOnly("participantContextId id was null");
    }

    @Test
    void validate_missingVerifiableCredential_shouldFailValidation() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.VC1_0_JWT, null))
                .build();

        var result = validator.validate(manifest);

        assertThat(result).isFailed().messages().containsOnly("VerifiableCredential was null");
    }

    @Test
    void validate_nullManifest_shouldFailValidation() {
        var result = validator.validate(null);

        assertThat(result).isFailed().messages().containsOnly("Input was null");
    }

    private static VerifiableCredentialManifest manifest(String rawVc, CredentialFormat format) {
        return VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .verifiableCredentialContainer(new VerifiableCredentialContainer(rawVc, format, VerifiableCredential.Builder.newInstance()
                        .type("type")
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .id("id")
                                .claim("key", "value")
                                .build())
                        .issuer(new Issuer("issuer"))
                        .issuanceDate(Instant.now())
                        .build()))
                .build();
    }

    private static String unsignedJwt(String payloadJson) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8)) + "." +
                encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8)) + ".AAAA";
    }
}
