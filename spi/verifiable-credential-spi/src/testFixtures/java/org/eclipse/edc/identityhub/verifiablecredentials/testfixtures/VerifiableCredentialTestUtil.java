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

package org.eclipse.edc.identityhub.verifiablecredentials.testfixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.security.token.jwt.CryptoConverter;

/**
 * Util class to manipulate VerifiableCredentials in tests.
 */
public class VerifiableCredentialTestUtil {

    private static final ECKeyGenerator EC_KEY_GENERATOR = new ECKeyGenerator(Curve.P_256);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VerifiableCredentialTestUtil() {
    }


    public static ECKey generateEcKey(String kid) {
        try {
            return EC_KEY_GENERATOR.keyUse(KeyUse.SIGNATURE).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }


    public static SignedJWT buildSignedJwt(JWTClaimsSet claims, ECKey jwk) {
        try {
            var signer = CryptoConverter.createSigner(jwk);
            var algorithm = CryptoConverter.getRecommendedAlgorithm(signer);
            var jwsHeader = new JWSHeader.Builder(algorithm)
                    .keyID(jwk.getKeyID())
                    .build();
            var jws = new SignedJWT(jwsHeader, claims);

            var jwsSigner = new ECDSASigner(jwk.toECPrivateKey());
            jws.sign(jwsSigner);

            var output = SignedJWT.parse(jws.serialize());
            output.getJWTClaimsSet();
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
