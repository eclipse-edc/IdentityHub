/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.creators;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

abstract class PresentationGeneratorTest {

    public static final String KEY_ID = "https://test.com/test-keys#key-1";

    @Test
    @DisplayName("Verify succesful creation of a JWT_VP")
    abstract void createPresentation_success();

    @Test
    @DisplayName("Should create a JWT_VP with VCs of different formats")
    abstract void create_whenVcsNotSameFormat();

    @Test
    @DisplayName("Should create a valid VP with no credential")
    abstract void create_whenVcsEmpty_shouldReturnEmptyVp();

    @Test
    @DisplayName("Should throw an exception if no key is found for a key-id")
    abstract void create_whenKeyNotFound();

    @Test
    @DisplayName("Should throw an exception if the required additional data is missing")
    abstract void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException();

    @Test
    @DisplayName("Should return an empty JWT when no credentials are passed")
    abstract void create_whenEmptyList();

    protected ECKey createKey(Curve p256, String centralIssuerKeyId) {
        try {
            return new ECKeyGenerator(p256)
                    .keyID(centralIssuerKeyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    protected VerifiableCredential createDummyCredential() {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test-subject")
                        .claim("test-claim", "test-value")
                        .build())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }
}