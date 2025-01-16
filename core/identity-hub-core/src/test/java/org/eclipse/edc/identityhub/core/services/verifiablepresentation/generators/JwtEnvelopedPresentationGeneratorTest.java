/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.TestData.ENVELOPED_CREDENTIAL_JSON;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtEnvelopedPresentationGeneratorTest extends PresentationGeneratorTest {
    private static final Map<String, Object> ADDITIONAL_DATA = Map.of(
            "controller", "did:web:test"
    );
    private final JwsSignerProvider signerProvider = mock();

    private final TokenGenerationService tokenGenerator = new JwtGenerationService(signerProvider);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeReference<Map<String, Object>> mapRef = new TypeReference<>() {
    };
    private JwtEnvelopedPresentationGenerator generator;

    @BeforeEach
    void setUp() throws JOSEException {
        var vpSigningKey = createKey(Curve.P_384, "vp-key");
        when(signerProvider.createJwsSigner(anyString())).thenReturn(Result.failure("not found"));
        when(signerProvider.createJwsSigner(eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(new ECDSASigner(vpSigningKey)));

        generator = new JwtEnvelopedPresentationGenerator(mock(), tokenGenerator);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Override
    void createPresentation_success() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, getCredential(ENVELOPED_CREDENTIAL_JSON));
        var vcc = new VerifiableCredentialContainer(jwtVc, CredentialFormat.VC2_0_JOSE, createDummyCredential());

        var vpJwt = generator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();

        var header = extractJwtHeader(vpJwt);
        assertThat(header.getKeyID()).isEqualTo("did:web:test#" + PUBLIC_KEY_ID);
        var claims = extractJwtClaims(vpJwt);

        assertThat(claims.getClaims()).hasSize(3)
                .containsKey("@context")
                .containsKey("id")
                .containsEntry("type", "EnvelopedVerifiablePresentation");

        var envelopedVp = claims.getClaim("id").toString();
        assertThat(envelopedVp).startsWith("data:application/vp+jwt,");

        var vpToken = envelopedVp.replace("data:application/vp+jwt", "");
        var vpClaims = extractJwtClaims(vpToken);
        assertThat(vpClaims.getClaims()).hasSize(4)
                .containsEntry("holder", issuerId)
                .containsEntry("type", "VerifiablePresentation")
                .containsKey("@context")
                .containsKey("verifiableCredential");

        // parse the credential tokens contained in the VP
        var credentials = (List<Map<String, Object>>) vpClaims.getClaim("verifiableCredential");

        assertThat(credentials).hasSize(1);
        assertThat(credentials).allSatisfy(c -> {
            assertThat(c.get("type")).isEqualTo("EnvelopedVerifiableCredential");
            assertThat(c).hasEntrySatisfying("id", v -> assertThat(v.toString()).startsWith("data:application/vc+jwt,"));
            var vcToken = c.get("id").toString().replace("data:application/vc+jwt,", "");

            // assert that all VC claims are present in their original form.
            var vcClaims = extractJwtClaims(vcToken);
            assertThat(vcClaims.getClaims())
                    .containsKeys("validFrom", "credentialSubject", "issuer", "type", "id", "@context");
        });
    }

    @Test
    @Override
    void create_whenVcsNotSameFormat() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, getCredential(ENVELOPED_CREDENTIAL_JSON));
        var vcc = new VerifiableCredentialContainer(jwtVc, CredentialFormat.VC1_0_JWT, createDummyCredential());

        assertThatThrownBy(() -> generator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can only handle credentials that are in VC2_0_JOSE format");
    }

    @Test
    @Override
    void create_whenPrivateKeyNotFound() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, getCredential(ENVELOPED_CREDENTIAL_JSON));
        var vcc = new VerifiableCredentialContainer(jwtVc, CredentialFormat.VC2_0_JOSE, createDummyCredential());

        assertThatThrownBy(() -> generator.generatePresentation(List.of(vcc), "some-nonexistent-key", PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA))
                .isInstanceOf(EdcException.class)
                .hasMessage("JWSSigner cannot be generated for private key 'some-nonexistent-key': not found");
    }

    @Test
    @Override
    void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException() {
        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, getCredential(ENVELOPED_CREDENTIAL_JSON));
        var vcc = new VerifiableCredentialContainer(jwtVc, CredentialFormat.VC2_0_JOSE, createDummyCredential());

        assertThatThrownBy(() -> generator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Must provide additional data: 'controller'");
    }

    @Test
    @Override
    void create_whenEmptyCredentialsList() {
        var vpJwt = generator.generatePresentation(List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(vpJwt).isNotNull();
        var header = extractJwtHeader(vpJwt);
        assertThat(header.getKeyID()).isEqualTo("did:web:test#" + PUBLIC_KEY_ID);
        var claims = extractJwtClaims(vpJwt);

        assertThat(claims.getClaims()).hasSize(3)
                .containsKey("@context")
                .containsKey("id")
                .containsEntry("type", "EnvelopedVerifiablePresentation");

        var envelopedVp = claims.getClaim("id").toString();
        assertThat(envelopedVp).startsWith("data:application/vp+jwt,");

        var vpToken = envelopedVp.replace("data:application/vp+jwt", "");
        var vpClaims = extractJwtClaims(vpToken);
        assertThat(vpClaims.getClaims()).hasSize(4)
                .containsEntry("holder", issuerId)
                .containsEntry("type", "VerifiablePresentation")
                .containsKey("@context")
                .containsKey("verifiableCredential");

        // parse the credential tokens contained in the VP
        var credentials = (List<Map<String, Object>>) vpClaims.getClaim("verifiableCredential");

        assertThat(credentials).describedAs("Credentials array should be empty").isEmpty();
    }


    @Test
    void generatePresentation_noAdditionalData() {
        assertThatThrownBy(() -> generator.generatePresentation(List.of(), "foobar-private", "foobar-public"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Map<String, Object> getCredential(String credentialString) {

        try {
            return objectMapper.readValue(credentialString, mapRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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