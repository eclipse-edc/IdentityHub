/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.JwtPresentationGenerator.VERIFIABLE_PRESENTATION_CLAIM;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.VERIFIABLE_CREDENTIAL_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtPresentationGeneratorTest extends PresentationGeneratorTest {
    public static final List<String> REQUIRED_CLAIMS = asList("aud", "exp", "iat", VERIFIABLE_PRESENTATION_CLAIM);
    private static final Map<String, Object> ADDITIONAL_DATA = Map.of(
            "aud", "did:web:test-audience",
            "controller", "did:web:test"
    );
    private final JwsSignerProvider signerProvider = mock();
    private final TokenGenerationService tokenGenerationService = new JwtGenerationService(signerProvider);
    private JwtPresentationGenerator creator;

    @BeforeEach
    void setup() throws JOSEException {
        var vpSigningKey = createKey(Curve.P_384, "vp-key");
        when(signerProvider.createJwsSigner(anyString())).thenReturn(Result.failure("not found"));
        when(signerProvider.createJwsSigner(eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(new ECDSASigner(vpSigningKey)));

        creator = new JwtPresentationGenerator(Clock.systemUTC(), tokenGenerationService);
    }

    @Override
    @Test
    @DisplayName("Verify succesful creation of a JWT_VP")
    void createPresentation_success() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, Map.of("vc", TestConstants.VC_CONTENT_DEGREE_EXAMPLE));
        var vcc = new VerifiableCredentialContainer(jwtVc, CredentialFormat.JWT, createDummyCredential());

        var vpJwt = creator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();
        assertThatNoException().isThrownBy(() -> SignedJWT.parse(vpJwt));
        var claims = extractJwtClaims(vpJwt);

        REQUIRED_CLAIMS.forEach(claim -> assertThat(claims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
        assertThat(claims.getClaim(VERIFIABLE_PRESENTATION_CLAIM)).isInstanceOfSatisfying(Map.class, vpClaim -> {
            assertThat(vpClaim.get(VERIFIABLE_CREDENTIAL_PROPERTY))
                    .isNotNull()
                    .isInstanceOfSatisfying(List.class, vcs -> assertThat(vcs).containsExactly(jwtVc));
            assertThat(extractJwtHeader(vpJwt).getKeyID()).isEqualTo("did:web:test#%s".formatted(PUBLIC_KEY_ID));
        });
    }

    @Override
    @Test
    @DisplayName("Should create a JWT_VP with VCs of different formats")
    void create_whenVcsNotSameFormat() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, Map.of("vc", TestConstants.VC_CONTENT_DEGREE_EXAMPLE));
        var ldpVc = TestData.LDP_VC_WITH_PROOF;

        var vc1 = new VerifiableCredentialContainer(jwtVc, CredentialFormat.JWT, createDummyCredential());
        var vc2 = new VerifiableCredentialContainer(ldpVc, CredentialFormat.JSON_LD, createDummyCredential());

        var vpJwt = creator.generatePresentation(List.of(vc1, vc2), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();
        assertThatNoException().isThrownBy(() -> SignedJWT.parse(vpJwt));

        var claims = extractJwtClaims(vpJwt);

        REQUIRED_CLAIMS.forEach(claim -> assertThat(claims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
    }

    @Override
    @Test
    @DisplayName("Should throw an exception if no private key is found for a key-id")
    void create_whenPrivateKeyNotFound() {
        var vcc = new VerifiableCredentialContainer("foobar", CredentialFormat.JWT, createDummyCredential());
        assertThatThrownBy(() -> creator.generatePresentation(List.of(vcc), "not-exist", PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA))
                .isInstanceOf(EdcException.class)
                .hasMessage("JWSSigner cannot be generated for private key 'not-exist': not found");
    }

    @Test
    @DisplayName("Should throw an exception if the required additional data is missing")
    @Override
    void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException() {
        var vcc = new VerifiableCredentialContainer("foobar", CredentialFormat.JWT, createDummyCredential());
        assertThatThrownBy(() -> creator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID))
                .describedAs("Expected exception when no additional data provided")
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> creator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, Map.of()))
                .describedAs("Expected exception when additional data does not contain expected value ('aud')")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Override
    @Test
    @DisplayName("Should return an empty JWT when no credentials are passed")
    void create_whenEmptyCredentialsList() {

        var vpJwt = creator.generatePresentation(List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();
        assertThatNoException().isThrownBy(() -> SignedJWT.parse(vpJwt));
        var claims = extractJwtClaims(vpJwt);

        REQUIRED_CLAIMS.forEach(claim -> assertThat(claims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
        assertThat(claims.getClaim("vp")).isNotNull();
    }

    @Test
    @DisplayName("Should create a valid VP with no credential")
    void create_whenVcsEmpty_shouldReturnEmptyVp() {
        var vpJwt = creator.generatePresentation(List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();
        assertThatNoException().isThrownBy(() -> SignedJWT.parse(vpJwt));

        var claims = extractJwtClaims(vpJwt);

        REQUIRED_CLAIMS.forEach(claim -> assertThat(claims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
    }

    private JWTClaimsSet extractJwtClaims(String vpJwt) {
        try {
            return SignedJWT.parse(vpJwt).getJWTClaimsSet();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private JWSHeader extractJwtHeader(String vpJwt) {
        try {
            return SignedJWT.parse(vpJwt).getHeader();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}