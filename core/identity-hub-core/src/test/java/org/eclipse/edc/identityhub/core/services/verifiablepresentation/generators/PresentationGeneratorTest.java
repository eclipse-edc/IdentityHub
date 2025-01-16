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

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

abstract class PresentationGeneratorTest {

    public static final String PRIVATE_KEY_ALIAS = "private-key";
    public static final String PUBLIC_KEY_ID = "key-1";
    protected final String issuerId = "did:web:testissuer";

    @Test
    @DisplayName("Verify succesful creation of a JWT_VP")
    abstract void createPresentation_success();

    @Test
    @DisplayName("Should create a JWT_VP with VCs of different formats")
    abstract void create_whenVcsNotSameFormat();

    @Test
    @DisplayName("Should throw an exception if no key is found for a key-id")
    abstract void create_whenPrivateKeyNotFound();

    @Test
    @DisplayName("Should throw an exception if the required additional data is missing")
    abstract void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException();

    @Test
    @DisplayName("Should return an empty, valid JWT when no credentials are passed")
    abstract void create_whenEmptyCredentialsList();

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