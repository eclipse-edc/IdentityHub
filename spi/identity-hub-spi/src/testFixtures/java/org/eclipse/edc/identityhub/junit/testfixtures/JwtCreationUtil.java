/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.junit.testfixtures;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;

import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

public class JwtCreationUtil {
    public static final String TEST_SCOPE = "org.eclipse.edc.vc.type:SomeTestCredential:read";
    public static final ECKey CONSUMER_KEY = generateEcKey();
    public static final ECKey PROVIDER_KEY = generateEcKey();

    public static String generateSiToken() {
        return generateSiToken("consumer-id", "did:web:consumer", "provider-id", "did:web:provider");
    }

    public static String generateSiToken(String consumerIdentifier, String consumerDid, String providerIdentifier, String providerDid) {
        var accessToken = generateJwt(consumerDid, consumerDid, providerIdentifier, Map.of("scope", TEST_SCOPE), CONSUMER_KEY);
        return generateJwt(consumerIdentifier, providerDid, providerDid, Map.of("client_id", providerIdentifier, "access_token", accessToken), PROVIDER_KEY);
    }

    public static String generateJwt(String aud, String iss, String sub, Map<String, String> otherClaims, ECKey ecKey) {
        var builder = new JWTClaimsSet.Builder()
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .audience(aud)
                .issuer(iss)
                .subject(sub)
                .jwtID(UUID.randomUUID().toString());

        otherClaims.forEach(builder::claim);

        var jwt = buildSignedJwt(builder.build(), ecKey);

        return jwt.serialize();
    }

}
