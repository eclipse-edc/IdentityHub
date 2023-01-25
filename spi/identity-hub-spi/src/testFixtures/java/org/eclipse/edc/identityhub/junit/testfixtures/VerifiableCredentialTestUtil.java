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

package org.eclipse.edc.identityhub.junit.testfixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialSubject;
import org.eclipse.edc.identityhub.spi.credentials.model.Proof;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Util class to manipulate VerifiableCredentials in tests.
 */
public class VerifiableCredentialTestUtil {

    private static final ECKeyGenerator EC_KEY_GENERATOR = new ECKeyGenerator(Curve.P_256);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VerifiableCredentialTestUtil() {
    }

    public static Credential generateCredential() {
        return Credential.Builder.newInstance()
                .context(VerifiableCredential.DEFAULT_CONTEXT)
                .id(UUID.randomUUID().toString())
                .issuer("issuer")
                .issuanceDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .type(VerifiableCredential.DEFAULT_TYPE)
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("id-test")
                        .claim("cred1", UUID.randomUUID().toString())
                        .claim("cred2", UUID.randomUUID().toString())
                        .build())
                .build();
    }

    public static Proof generateProof() {
        return Proof.Builder.newInstance()
                .verificationMethod("verificationMethod")
                .proofPurpose("proofPurpose")
                .created(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .type("type")
                .build();
    }

    public static ECKey generateEcKey() {
        try {
            return EC_KEY_GENERATOR.keyUse(KeyUse.SIGNATURE).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static SignedJWT buildSignedJwt(Credential credential, String issuer, String subject, ECKey jwk) {
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", MAPPER.convertValue(credential, Map.class))
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

            var output = SignedJWT.parse(jws.serialize());
            output.getJWTClaimsSet();
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
