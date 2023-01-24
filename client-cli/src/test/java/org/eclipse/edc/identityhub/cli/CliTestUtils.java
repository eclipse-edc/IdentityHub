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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;

import static org.eclipse.edc.identityhub.cli.CryptoUtils.readEcKeyPemFile;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;

class CliTestUtils {
    
    public static final String PUBLIC_KEY_PATH = "src/test/resources/test-public-key.pem";
    public static final String PRIVATE_KEY_PATH = "src/test/resources/test-private-key.pem";
    public static final ECKey PUBLIC_KEY;
    public static final ECKey PRIVATE_KEY;

    static {
        try {
            PUBLIC_KEY = readEcKeyPemFile(PUBLIC_KEY_PATH);
            PRIVATE_KEY = readEcKeyPemFile(PRIVATE_KEY_PATH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CliTestUtils() {
    }

    public static SignedJWT toJwtVerifiableCredential(Credential vc) {
        try {

            return buildSignedJwt(vc,
                    "identity-hub-test-issuer",
                    "identity-hub-test-subject",
                    PRIVATE_KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyVerifiableCredentialSignature(SignedJWT jwt) {
        try {
            var wrapper = new EcPublicKeyWrapper(PUBLIC_KEY);
            return jwt.verify(wrapper.verifier());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}
