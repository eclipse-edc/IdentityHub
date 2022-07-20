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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.identityhub.model.credentials.VerifiableCredential;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Util class to manipulate VerifiableCredentials in tests.
 */
public class VerifiableCredentialTestUtil {

    public static final String VC_AUDIENCE = "identity-hub";
    public static final Date EXP = Date.from(Instant.now().plus(Duration.ofDays(1)));

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

    public static ECKey generateEcKey() throws Exception {
        return EC_KEY_GENERATOR.keyUse(KeyUse.SIGNATURE).generate();
    }

    public static PublicKeyWrapper toPublicKeyWrapper(ECKey jwk) {
        var publicKey = new EllipticCurvePublicKey(jwk.getCurve().getName(), jwk.getKeyType().getValue(), jwk.getX().toString(), jwk.getY().toString());
        return KeyConverter.toPublicKeyWrapper(publicKey, "ec");
    }

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer) throws Exception {
        var jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();
        return buildSignedJwt(credential, issuer, jwk);
    }

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, ECKey jwk) throws Exception {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", credential)
                .issuer(issuer)
                .audience(VC_AUDIENCE)
                .expirationTime(EXP)
                .subject("verifiable-credential")
                .build();

        var jws = new SignedJWT(jwsHeader, claims);

        var jwsSigner = new ECDSASigner(jwk.toECPrivateKey());
        jws.sign(jwsSigner);

        return SignedJWT.parse(jws.serialize());
    }

    public static Map<String, Object> toMap(VerifiableCredential verifiableCredential, String issuer) {
        return Map.of(verifiableCredential.getId(),
                Map.of("vc", Map.of("credentialSubject", verifiableCredential.getCredentialSubject(),
                                "id", verifiableCredential.getId()),
                "aud", VC_AUDIENCE,
                "sub", "verifiable-credential",
                "iss", issuer,
                "exp", EXP));
    }
}
