/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.cli;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;

import java.io.File;
import java.util.Map;

public class TestUtils {
    static final Faker FAKER = new Faker();
    public static ECKey PUBLIC_KEY;
    public static ECKey PRIVATE_KEY;

    static {
        try {
            PUBLIC_KEY = JWTUtils.readECKey(new File("src/test/resources/test-public-key.pem"));
            PRIVATE_KEY = JWTUtils.readECKey(new File("src/test/resources/test-private-key.pem"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestUtils() {
    }

    public static VerifiableCredential createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .credentialSubject(Map.of(
                        FAKER.internet().uuid(), FAKER.lorem().word(),
                        FAKER.internet().uuid(), FAKER.lorem().word()))
                .build();
    }

    public static SignedJWT getSignedVerifiableCredential(VerifiableCredential vc) {
        try {

            return JWTUtils.buildSignedJwt(
                    vc,
                    "identity-hub",
                    PRIVATE_KEY.toECPrivateKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyVerifiableCredentialSignature(SignedJWT jwt) {
        try {
            return jwt.verify(new ECDSAVerifier(PUBLIC_KEY.toECPublicKey()));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}