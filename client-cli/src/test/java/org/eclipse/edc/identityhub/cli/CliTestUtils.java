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

package org.eclipse.edc.identityhub.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.identityhub.verifier.jwt.VerifiableCredentialsJwtService;
import org.eclipse.edc.identityhub.verifier.jwt.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.credentials.CryptoUtils.readPrivateEcKey;
import static org.eclipse.edc.identityhub.spi.credentials.CryptoUtils.readPublicEcKey;
import static org.mockito.Mockito.mock;

class CliTestUtils {
    public static final String PUBLIC_KEY_PATH = "src/test/resources/test-public-key.pem";
    public static final String PRIVATE_KEY_PATH = "src/test/resources/test-private-key.pem";
    public static final PublicKeyWrapper PUBLIC_KEY;
    public static final PrivateKeyWrapper PRIVATE_KEY;
    private static final VerifiableCredentialsJwtService VC_JWT_SERVICE = new VerifiableCredentialsJwtServiceImpl(new ObjectMapper(), mock(Monitor.class));

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
                .id(UUID.randomUUID().toString())
                .credentialSubject(Map.of(
                        UUID.randomUUID().toString(), "value1",
                        UUID.randomUUID().toString(), "value2"))
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
