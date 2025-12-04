/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.tests.fixtures.common;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.nimbusds.jose.JWSAlgorithm.ES256;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub.SUPER_USER;

/**
 * This class is a test extension, that can be registered with {@code @RegisterExtension} to facilitate OAuth2 authorization.
 * <p>
 * It generates a server signing key and prepares a {@link WireMockExtension} to host a JWKS endpoint where this key
 * can be retrieved. This is important for token verification. In addition, it offers methods to create OAuth2 tokens that
 * are signed with that same key.
 */
public class Oauth2Extension implements BeforeAllCallback, BeforeEachCallback, Oauth2TokenProvider {
    private final WireMockExtension mockJwksServer;
    private final String signingKeyId;
    private final String someIssuer;
    private ECKey oauthServerSigningKey;

    public Oauth2Extension(WireMockExtension mockJwksServer) {
        this(mockJwksServer, UUID.randomUUID().toString());
    }

    public Oauth2Extension(WireMockExtension mockJwksServer, String signingKeyId) {
        this(mockJwksServer, signingKeyId, "someissuer");
    }

    public Oauth2Extension(WireMockExtension mockJwksServer, String signingKeyId, String issuer) {
        this.mockJwksServer = mockJwksServer;
        this.signingKeyId = signingKeyId;
        this.someIssuer = issuer;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // this has to be static, because the runtime will cache JWKS
        oauthServerSigningKey = new ECKeyGenerator(Curve.P_256).keyID(signingKeyId).generate();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // create JWKS with the participant's key
        var jwks = createObjectBuilder()
                .add("keys", createArrayBuilder().add(createObjectBuilder(
                        oauthServerSigningKey.toPublicJWK().toJSONObject())))
                .build()
                .toString();
        // use wiremock to host a JWKS endpoint
        mockJwksServer.stubFor(any(urlPathEqualTo("/.well-known/jwks.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwks)));
    }

    public String signClaims(HashMap<String, Object> claims) {
        var claimsBuilder = new JWTClaimsSet.Builder();
        claims.forEach(claimsBuilder::claim);
        var hdr = new JWSHeader.Builder(ES256).keyID(oauthServerSigningKey.getKeyID()).build();
        var jwt = new SignedJWT(hdr, claimsBuilder.build());
        try {
            var signer = new ECDSASigner(oauthServerSigningKey);
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
        return jwt.serialize();
    }

    @NotNull
    public String createToken(String participantContextId) {
        var role = ParticipantPrincipal.ROLE_PARTICIPANT;
        if (SUPER_USER.equals(participantContextId)) {
            role = ParticipantPrincipal.ROLE_ADMIN;
        }
        var claims = new HashMap<String, Object>(Map.of(
                "sub", "test-subject",
                "iss", someIssuer,
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond(),
                "jti", UUID.randomUUID().toString(),
                "scope", "management-api:read management-api:write identity-api:read identity-api:write",
                "role", role,
                "participant_context_id", participantContextId
        ));

        return signClaims(claims);
    }
}
