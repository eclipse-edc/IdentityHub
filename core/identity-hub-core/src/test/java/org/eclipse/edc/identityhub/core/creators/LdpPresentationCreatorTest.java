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

package org.eclipse.edc.identityhub.core.creators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.TestConstants;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.JWS_2020_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdpPresentationCreatorTest extends PresentationCreatorTest {

    private final PrivateKeyResolver resolverMock = mock();
    private final Map<String, Object> types = Map.of("types", List.of("VerifiablePresentation", "SomeOtherPresentationType"));
    private LdpPresentationCreator creator;

    @BeforeEach
    void setup() throws URISyntaxException {
        var vpSigningKey = createKey(KEY_ID);
        when(resolverMock.resolvePrivateKey(eq(KEY_ID), any())).thenReturn(new OctetKeyPairWrapper(vpSigningKey));
        var signatureSuiteRegistryMock = mock(SignatureSuiteRegistry.class);
        when(signatureSuiteRegistryMock.getForId(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE)).thenReturn(new JwsSignature2020Suite(new ObjectMapper()));
        var ldpIssuer = LdpIssuer.Builder.newInstance()
                .jsonLd(initializeJsonLd())
                .monitor(mock())
                .build();
        creator = new LdpPresentationCreator(resolverMock, "did:web:test-issuer", signatureSuiteRegistryMock, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, ldpIssuer,
                JacksonJsonLd.createObjectMapper());
    }

    @Override
    @Test
    public void createPresentation_success() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.JSON_LD, createDummyCredential());

        var result = creator.createPresentation(List.of(vcc), KEY_ID, types);
        assertThat(result).isNotNull();
        assertThat(result.get("https://w3id.org/security#proof")).isNotNull();
    }

    @Override
    @Test
    public void create_whenVcsNotSameFormat() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.JSON_LD, createDummyCredential());

        var vcSigningKey = createKey(Curve.P_256, TestConstants.CENTRAL_ISSUER_KEY_ID);
        var jwtVc = JwtCreationUtils.createJwt(vcSigningKey, TestConstants.CENTRAL_ISSUER_DID, "degreeSub", TestConstants.VP_HOLDER_ID, Map.of("vc", TestConstants.VC_CONTENT_DEGREE_EXAMPLE));
        var vcc2 = new VerifiableCredentialContainer(jwtVc, CredentialFormat.JWT, createDummyCredential());

        assertThatThrownBy(() -> creator.createPresentation(List.of(vcc, vcc2), KEY_ID, types))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("One or more VerifiableCredentials cannot be represented in the desired format %s".formatted(CredentialFormat.JSON_LD));
    }

    @Override
    @Test
    public void create_whenVcsEmpty_shouldReturnEmptyVp() {
        var result = creator.createPresentation(List.of(), KEY_ID, types);
        assertThat(result).isNotNull();
    }

    @Override
    @Test
    public void create_whenKeyNotFound() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.JSON_LD, createDummyCredential());
        assertThatThrownBy(() -> creator.createPresentation(List.of(vcc), "not-exists", types))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No key could be found with key ID 'not-exists'");
    }

    @Override
    @Test
    public void create_whenRequiredAdditionalDataMissing_throwsIllegalArgumentException() {
        var ldpVc = TestData.LDP_VC_WITH_PROOF;
        var vcc = new VerifiableCredentialContainer(ldpVc, CredentialFormat.JSON_LD, createDummyCredential());
        assertThatThrownBy(() -> creator.createPresentation(List.of(vcc), KEY_ID)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Must provide additional data: 'types'");

        assertThatThrownBy(() -> creator.createPresentation(List.of(vcc), KEY_ID, Map.of("some-key", "some-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Must provide additional data: 'types'");
    }

    @Test
    @DisplayName("Should return an empty JWT when no credentials are passed")
    @Override
    void create_whenEmptyList() {

        var result = creator.createPresentation(List.of(), KEY_ID, types);
        assertThat(result).isNotNull();
        assertThat(result.get("https://w3id.org/security#proof")).isNotNull();
    }

    private OctetKeyPair createKey(String keyId) {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .keyID(keyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private TitaniumJsonLd initializeJsonLd() {
        var jld = new TitaniumJsonLd(mock());
        jld.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", TestUtils.getResource("odrl.jsonld"));
        jld.registerCachedDocument(DID_CONTEXT_URL, TestUtils.getResource("did.json"));
        jld.registerCachedDocument(JWS_2020_URL, TestUtils.getResource("jws2020.json"));
        jld.registerCachedDocument(W3C_CREDENTIALS_URL, TestUtils.getResource("credentials.v1.json"));
        jld.registerCachedDocument(IATP_CONTEXT_URL, TestUtils.getResource("presentation-query.v08.json"));
        jld.registerCachedDocument(PRESENTATION_EXCHANGE_URL, TestUtils.getResource("presentation-exchange.v1.json"));
        jld.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", TestUtils.getResource("examples.v1.json"));
        return jld;
    }

    private record OctetKeyPairWrapper(OctetKeyPair privateKey) implements PrivateKeyWrapper {

        @Override
        public JWEDecrypter decrypter() {
            return null; // not needed here
        }

        @Override
        public JWSSigner signer() {
            try {
                return new Ed25519Signer(privateKey);
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
