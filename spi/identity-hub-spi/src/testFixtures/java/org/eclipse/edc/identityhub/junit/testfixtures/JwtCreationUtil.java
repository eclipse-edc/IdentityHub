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

/**
 * Utility class to generate various JWTs
 */
public class JwtCreationUtil {
    public static final String TEST_SCOPE = "org.eclipse.edc.vc.type:SomeTestCredential:read";
    public static final ECKey CONSUMER_KEY = generateEcKey("did:web:consumer#key1");
    public static final ECKey PROVIDER_KEY = generateEcKey("did:web:provider#key1");

    /**
     * Generates a self-issued token.
     *
     * @return The generated self-issued token.
     */
    public static String generateSiToken() {
        return generateSiToken("consumer-id", "did:web:consumer", "provider-id", "did:web:provider");
    }

    /**
     * Generates a self-issued token.
     *
     * @param consumerIdentifier The consumer participant agent identifier.
     * @param consumerDid        The consumer DID.
     * @param providerIdentifier The provider participant agent identifier.
     * @param providerDid        The provider DID.
     * @return The generated self-issued token.
     */
    public static String generateSiToken(String consumerIdentifier, String consumerDid, String providerIdentifier, String providerDid) {
        var accessToken = generateJwt(consumerDid, consumerDid, providerIdentifier, Map.of("scope", TEST_SCOPE), CONSUMER_KEY);
        return generateJwt(consumerIdentifier, providerDid, providerDid, Map.of("client_id", providerIdentifier, "access_token", accessToken), PROVIDER_KEY);
    }

    /**
     * Generates a JSON Web Token (JWT) with the specified claims and signs it using the provided ECKey.
     *
     * @param aud         The audience claim, which identifies the intended recipients of the JWT.
     * @param iss         The issuer claim, which identifies the entity that issued the JWT.
     * @param sub         The subject claim, which identifies the principal that is the subject of the JWT.
     * @param otherClaims Additional claims to include in the JWT. The keys represent the claim names and the values represent the claim values.
     * @param ecKey       The private key used to sign the JWT.
     * @return The generated JWT as a serialized string.
     */
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
