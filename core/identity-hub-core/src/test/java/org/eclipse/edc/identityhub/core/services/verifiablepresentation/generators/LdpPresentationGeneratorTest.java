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

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.method.resolver.MethodResolver;
import com.apicatalog.vc.proof.Proof;
import com.nimbusds.jose.jwk.Curve;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.TestConstants;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apicatalog.vc.VcVocab.SECURITY_VOCAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.JWS_2020_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdpPresentationGeneratorTest extends PresentationGeneratorTest {
    private static final Map<String, Object> ADDITIONAL_DATA = Map.of(
            "types", List.of("VerifiablePresentation", "SomeOtherPresentationType"),
            "controller", "did:web:test"
    );
    private static final Instant NOW = Instant.parse("2020-01-01T10:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(5);

    private final PrivateKeyResolver privateKeyResolver = mock();
    private final TypeManager typeManager = mock();
    private final String participantContextId = "test-participant";
    private final SignatureSuiteRegistry signatureSuiteRegistryMock = mock();
    private LdpPresentationGenerator creator;
    private KeyPair vpSigningKey;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        vpSigningKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

        when(privateKeyResolver.resolvePrivateKey(eq(participantContextId), any())).thenReturn(Result.failure("no key found"));
        when(privateKeyResolver.resolvePrivateKey(eq(participantContextId), eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(vpSigningKey.getPrivate()));
        var suite = new Jws2020SignatureSuite(new ObjectMapper());
        when(signatureSuiteRegistryMock.getForId(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE)).thenReturn(suite);
        when(signatureSuiteRegistryMock.getAllSuites()).thenReturn(List.of(suite));

        var ldpIssuer = LdpIssuer.Builder.newInstance()
                .jsonLd(initializeJsonLd())
                .monitor(mock())
                .build();
        creator = new LdpPresentationGenerator(privateKeyResolver, signatureSuiteRegistryMock, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, ldpIssuer,
                typeManager, "test", Clock.fixed(NOW, ZoneOffset.UTC), LIFETIME);

        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Override
    @Test
    public void createPresentation_success() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(result).isNotNull();
        assertThat(result.get("https://w3id.org/security#proof")).isNotNull();
    }

    @Test
    void createPresentation_bindsProofToAudienceAndLifetime() {
        var additionalData = new HashMap<>(ADDITIONAL_DATA);
        additionalData.put("aud", "did:web:verifier");

        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, additionalData);

        var proof = proof(result);
        assertThat(proofValue(proof, "domain")).isEqualTo("did:web:verifier");
        assertThat(proofValue(proof, "expiration")).isEqualTo(NOW.plus(LIFETIME).toString());
    }

    @Test
    void createPresentation_isVerifiable() {
        var controller = ADDITIONAL_DATA.get("controller").toString();
        var presentation = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, controller, ADDITIONAL_DATA);

        // the holder's DID document, which publishes the public key under the verification method that the proof references
        var publicKey = CryptoConverter.createJwk(new KeyPair(vpSigningKey.getPublic(), null)).toPublicJWK();
        var didDocument = new MethodResolver() {
            @Override
            public VerificationMethod resolve(URI id, DocumentLoader loader, Proof proof) {
                return new JsonWebKeyPair(id, LdpPresentationGenerator.JSON_WEB_KEY_2020, URI.create(controller), publicKey);
            }

            @Override
            public boolean isAccepted(URI id) {
                return id.toString().equals(controller + "#" + PUBLIC_KEY_ID);
            }
        };
        var verifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(signatureSuiteRegistryMock)
                .jsonLd(initializeJsonLd())
                .typeManager(typeManager)
                .typeContext("test")
                .methodResolvers(List.of(didDocument))
                .clock(Clock.fixed(NOW, ZoneOffset.UTC))
                .build();

        assertThat(verifier.verify(presentation.toString(), VerifierContext.Builder.newInstance().verifier(verifier).build())).isSucceeded();
    }

    @Test
    void createPresentation_proofPurposeIsAuthentication() {
        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        var purpose = proof(result).getJsonArray(SECURITY_VOCAB + "proofPurpose").getJsonObject(0);
        assertThat(purpose.getString("@id")).isEqualTo(SECURITY_VOCAB + "authentication");
    }

    @Test
    void createPresentation_proofHasNoChallenge() {
        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(proof(result)).doesNotContainKey(SECURITY_VOCAB + "challenge");
    }

    @Test
    void createPresentation_verificationMethodIsJsonWebKey2020() {
        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        var verificationMethod = proof(result).getJsonArray(SECURITY_VOCAB + "verificationMethod").getJsonObject(0);
        assertThat(verificationMethod.getJsonArray("@type").getString(0)).isEqualTo(SECURITY_VOCAB + "JsonWebKey2020");
    }

    @Test
    void createPresentation_withoutAudience_proofHasNoDomain() {
        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        var proof = proof(result);
        assertThat(proof).doesNotContainKey(SECURITY_VOCAB + "domain");
        assertThat(proofValue(proof, "expiration")).isEqualTo(NOW.plus(LIFETIME).toString());
    }

    @Override
    @Test
    public void create_whenVcsNotSameFormat() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.VC1_0_LD, createDummyCredential());

        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, Map.of("vc", TestConstants.VC_CONTENT_DEGREE_EXAMPLE));
        var vcc2 = new VerifiableCredentialContainer(jwtVc, CredentialFormat.VC1_0_JWT, createDummyCredential());

        assertThatThrownBy(() -> creator.generatePresentation(participantContextId, List.of(vcc, vcc2), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("One or more VerifiableCredentials cannot be represented in the desired format %s".formatted(CredentialFormat.VC1_0_LD));
    }

    @Override
    @Test
    public void create_whenPrivateKeyNotFound() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.VC1_0_LD, createDummyCredential());

        assertThatThrownBy(() -> creator.generatePresentation(participantContextId, List.of(vcc), "not-exists", PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Override
    @Test
    public void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.VC1_0_LD, createDummyCredential());
        assertThatThrownBy(() -> creator.generatePresentation(List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Must provide additional data: 'types' and 'controller'");

        assertThatThrownBy(() -> creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, Map.of("some-key", "some-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Must provide additional data: 'types'");

        assertThatThrownBy(() -> creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, Map.of("types", "some-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Must provide additional data: 'controller'");
    }

    @Test
    @DisplayName("Should return an empty JWT when no credentials are passed")
    @Override
    void create_whenEmptyCredentialsList() {

        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(result).isNotNull();
        assertThat(result.get("https://w3id.org/security#proof")).isNotNull();
    }

    @Test
    public void create_whenVcsEmpty_shouldReturnEmptyVp() {
        var result = creator.generatePresentation(participantContextId, List.of(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);
        assertThat(result).isNotNull();
    }

    @Test
    public void create_whenPublicKeyContainsController() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.VC1_0_LD, createDummyCredential());
        var publicKeyIdWithController = ADDITIONAL_DATA.get("controller").toString() + "#" + PUBLIC_KEY_ID;

        var result = creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, publicKeyIdWithController, issuerId, ADDITIONAL_DATA);
        assertThat(result).isNotNull();
        assertThat(result.get("https://w3id.org/security#proof")).isNotNull();
    }

    @NotNull
    private TitaniumJsonLd initializeJsonLd() {
        var jld = new TitaniumJsonLd(mock());
        jld.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", TestUtils.getResource("odrl.jsonld"));
        jld.registerCachedDocument(DID_CONTEXT_URL, TestUtils.getResource("did.json"));
        jld.registerCachedDocument(JWS_2020_URL, TestUtils.getResource("jws2020.json"));
        jld.registerCachedDocument(W3C_CREDENTIALS_URL, TestUtils.getResource("credentials.v1.json"));
        jld.registerCachedDocument(PRESENTATION_EXCHANGE_URL, TestUtils.getResource("presentation-exchange.v1.json"));
        jld.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", TestUtils.getResource("examples.v1.json"));
        return jld;
    }


    private static JsonObject proof(JsonObject presentation) {
        return presentation.getJsonArray(SECURITY_VOCAB + "proof").getJsonObject(0).getJsonArray("@graph").getJsonObject(0);
    }

    private static String proofValue(JsonObject proof, String term) {
        return proof.getJsonArray(SECURITY_VOCAB + term).getJsonObject(0).getString("@value");
    }
}
