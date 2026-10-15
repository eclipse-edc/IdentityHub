/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class SelfIssuedTokenPrecheckTest {

    private static final String ISSUER = "did:web:issuer.example.com";

    @Test
    void check_kidBoundToIssuer_succeeds() {
        var token = token(ISSUER + "#key-1", ISSUER);

        assertThat(SelfIssuedTokenPrecheck.check(token)).isSucceeded()
                .satisfies(unverified -> {
                    org.assertj.core.api.Assertions.assertThat(unverified.keyId()).isEqualTo(ISSUER + "#key-1");
                    org.assertj.core.api.Assertions.assertThat(unverified.issuer()).isEqualTo(ISSUER);
                });
    }

    @Test
    void check_kidOfOtherDid_fails() {
        var token = token("did:web:internal.host#key-1", ISSUER);

        assertThat(SelfIssuedTokenPrecheck.check(token)).isFailed().detail().startsWith("kid header");
    }

    @Test
    void check_kidExtendsIssuer_fails() {
        // the DID in 'kid' is 'did:web:issuer.example.com.internal', which merely starts with the issuer
        var token = token(ISSUER + ".internal#key-1", ISSUER);

        assertThat(SelfIssuedTokenPrecheck.check(token)).isFailed().detail().startsWith("kid header");
    }

    @Test
    void check_issuerComparedLiterally_fails() {
        // the '.' in the issuer must not match any other character of the 'kid'
        var token = token("did:web:issuerxexample.com#key-1", ISSUER);

        assertThat(SelfIssuedTokenPrecheck.check(token)).isFailed().detail().startsWith("kid header");
    }

    @Test
    void check_kidWithEmptyFragment_fails() {
        assertThat(SelfIssuedTokenPrecheck.check(token(ISSUER + "#", ISSUER))).isFailed().detail().startsWith("kid header");
    }

    @Test
    void check_issuerInvalidDid_fails() {
        var issuer = "(a+)+$";
        assertThat(SelfIssuedTokenPrecheck.check(token(issuer + "#key-1", issuer))).isFailed()
                .detail().isEqualTo("Claim 'iss' is not a valid DID.");
    }

    @Test
    void check_missingKid_fails() {
        assertThat(SelfIssuedTokenPrecheck.check(token(null, ISSUER))).isFailed().detail().isEqualTo("Kid not present");
    }

    @Test
    void check_missingIssuer_fails() {
        assertThat(SelfIssuedTokenPrecheck.check(token(ISSUER + "#key-1", null))).isFailed().detail().isEqualTo("Issuer claim not present");
    }

    @Test
    void check_malformedToken_fails() {
        assertThat(SelfIssuedTokenPrecheck.check("not-a-jwt")).isFailed().detail().isEqualTo("Failed to decode token");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "did:web:example.com",
            "did:web:sub.example-host.com",
            "did:web:localhost%3A8080",
            "did:web:localhost%3a8080:participants:alice",
            "did:web:example.com:user_1:alice.bob",
            "did:example:123%2F456" // other methods are not checked
    })
    void checkDid_wellFormed_succeeds(String did) {
        assertThat(SelfIssuedTokenPrecheck.checkDid(did)).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "did:web:",
            "did:web:example.com%2Fadmin",
            "did:web:example.com%3Fx=",
            "did:web:example.com%23",
            "did:web:user%40internal.host",
            "did:web:example.com/admin",
            "did:web:example.com?query",
            "did:web:user@internal.host",
            "did:web:example.com%3A",
            "did:web:example.com%3A80%3A81",
            "did:web:example.com::path",
            "did:web:example.com:",
            "did:web:exa mple.com",
            "did:web:example.com:path%3A8080"
    })
    void checkDid_malformed_fails(String did) {
        assertThat(SelfIssuedTokenPrecheck.checkDid(did)).isFailed().detail().startsWith("Malformed did:web identifier");
    }

    @Test
    void check_malformedDidWebIssuer_fails() {
        var issuer = "did:web:example.com%2Fadmin";
        var token = token(issuer + "#key-1", issuer);

        assertThat(SelfIssuedTokenPrecheck.check(token)).isFailed().detail().startsWith("Malformed did:web identifier");
    }

    private String token(String kid, String issuer) {
        try {
            ECKey key = new ECKeyGenerator(Curve.P_256).generate();
            var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).build();
            var jwt = new SignedJWT(header, new JWTClaimsSet.Builder().issuer(issuer).subject(issuer).build());
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
