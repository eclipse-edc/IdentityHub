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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;

import java.util.Map;

import static org.eclipse.dataspaceconnector.identityhub.credentials.CryptoUtils.readPrivateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.credentials.CryptoUtils.readPublicEcKey;


public class CliTestUtils {
    static final Faker FAKER = new Faker();
    public static final String PUBLIC_KEY_PATH = "src/test/resources/test-public-key.pem";
    public static final String PRIVATE_KEY_PATH = "src/test/resources/test-private-key.pem";
    public static final PublicKeyWrapper PUBLIC_KEY;
    public static final PrivateKeyWrapper PRIVATE_KEY;
    private static final VerifiableCredentialsJwtService VC_JWT_SERVICE = new VerifiableCredentialsJwtServiceImpl(new ObjectMapper());

    static {
        try {
            PUBLIC_KEY = readPublicEcKey(PUBLIC_KEY_PATH);
            PRIVATE_KEY = readPrivateEcKey(PRIVATE_KEY_PATH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CliTestUtils() {
    }

    public static VerifiableCredential createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .credentialSubject(Map.of(
                        FAKER.internet().uuid(), FAKER.lorem().word(),
                        FAKER.internet().uuid(), FAKER.lorem().word()))
                .build();
    }

    public static SignedJWT signVerifiableCredential(VerifiableCredential vc) {
        try {

            return VC_JWT_SERVICE.buildSignedJwt(
                    vc,
                    "identity-hub-test-issuer",
                    "identity-hub-test-subject",
                    PRIVATE_KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyVerifiableCredentialSignature(SignedJWT jwt) {
        try {
            return jwt.verify(PUBLIC_KEY.verifier());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}