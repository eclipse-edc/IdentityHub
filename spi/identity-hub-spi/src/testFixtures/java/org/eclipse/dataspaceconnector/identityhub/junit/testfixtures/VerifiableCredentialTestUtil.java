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

package org.eclipse.dataspaceconnector.identityhub.junit.testfixtures;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;

import java.util.Map;

/**
 * Util class to manipulate VerifiableCredentials in tests.
 */
public class VerifiableCredentialTestUtil {
    private static final Faker FAKER = new Faker();
    private static final ECKeyGenerator EC_KEY_GENERATOR = new ECKeyGenerator(Curve.P_256);

    public static VerifiableCredential generateVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .credentialSubject(Map.of(
                        FAKER.internet().uuid(), FAKER.lorem().word(),
                        FAKER.internet().uuid(), FAKER.lorem().word()))
                .build();
    }

    public static ECKey generateEcKey() {
        try {
            return EC_KEY_GENERATOR.keyUse(KeyUse.SIGNATURE).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKeyWrapper toPublicKeyWrapper(ECKey jwk) {
        return new EcPublicKeyWrapper(jwk);
    }

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, String subject, ECKey jwk) {
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", credential)
                .issuer(issuer)
                .subject(subject)
                .expirationTime(null)
                .notBeforeTime(null)
                .build();

        return buildSignedJwt(claims, jwk);
    }

    public static SignedJWT buildSignedJwt(JWTClaimsSet claims, ECKey jwk) {
        try {
            var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
            var jws = new SignedJWT(jwsHeader, claims);

            var jwsSigner = new ECDSASigner(jwk.toECPrivateKey());
            jws.sign(jwsSigner);

            return SignedJWT.parse(jws.serialize());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> toMap(VerifiableCredential verifiableCredential, String issuer, String subject) {
        return Map.of(verifiableCredential.getId(),
                Map.of("vc", Map.of("credentialSubject", verifiableCredential.getCredentialSubject(),
                                "id", verifiableCredential.getId()),
                "sub", subject,
                "iss", issuer));
    }
}
