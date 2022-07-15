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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.models.credentials.VerifiableCredential;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;

public class VerifiableCredentialTestUtil {

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer) throws Exception {
        var jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();
        return buildSignedJwt(credential, issuer, jwk);
    }

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, ECKey jwk) throws Exception {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", credential)
                .issuer(issuer)
                .audience("identity-hub")
                .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
                .subject("verifiable-credential")
                .build();
        var jws = new SignedJWT(jwsHeader, claims);

        var jwsSigner = new ECDSASigner(jwk.toECPrivateKey());
        jws.sign(jwsSigner);

        return SignedJWT.parse(jws.serialize());
    }
}
