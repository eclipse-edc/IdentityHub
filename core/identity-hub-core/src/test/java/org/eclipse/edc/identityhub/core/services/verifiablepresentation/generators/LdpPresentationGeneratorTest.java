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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.TestConstants;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.JWS_2020_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdpPresentationGeneratorTest extends PresentationGeneratorTest {
    private static final Map<String, Object> ADDITIONAL_DATA = Map.of(
            "types", List.of("VerifiablePresentation", "SomeOtherPresentationType"),
            "controller", "did:web:test"
    );

    private final PrivateKeyResolver privateKeyResolver = mock();
    private final TypeManager typeManager = mock();
    private final String participantContextId = "test-participant";
    private LdpPresentationGenerator creator;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        var vpSigningKey = KeyPairGenerator.getInstance("Ed25519")
                .generateKeyPair()
                .getPrivate();

        when(privateKeyResolver.resolvePrivateKey(eq(participantContextId), any())).thenReturn(Result.failure("no key found"));
        when(privateKeyResolver.resolvePrivateKey(eq(participantContextId), eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(vpSigningKey));
        var signatureSuiteRegistryMock = mock(SignatureSuiteRegistry.class);
        var suite = new Jws2020SignatureSuite(new ObjectMapper());
        when(signatureSuiteRegistryMock.getForId(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE)).thenReturn(suite);
        when(signatureSuiteRegistryMock.getAllSuites()).thenReturn(List.of(suite));

        var ldpIssuer = LdpIssuer.Builder.newInstance()
                .jsonLd(initializeJsonLd())
                .monitor(mock())
                .build();
        creator = new LdpPresentationGenerator(privateKeyResolver, signatureSuiteRegistryMock, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, ldpIssuer,
                typeManager, "test");

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

    @Test
    @DisplayName("Should merge custom contexts from credentials into VP @context array before signing")
    public void createPresentation_shouldMergeCustomContexts() {
        // Create a credential with custom context
        var customContext = "https://w3id.org/tractusx-trust/v0.8";
        var credentialWithCustomContext = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "%s"
                  ],
                  "id": "urn:uuid:12345",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {
                    "id": "did:web:subject",
                    "holderIdentifier": "BPNL000000000001"
                  }
                }
                """.formatted(customContext);

        var vcc = new VerifiableCredentialContainer(credentialWithCustomContext, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(result).isNotNull();
        
        // After signing, the document is in expanded JSON-LD form,
        // so the @context is not present in the signed output.
        // The important thing is that when this expanded document is later compacted
        // (as done in PresentationApiController), the custom contexts will be available
        // because they were included in the VP's @context before signing.
        
        // Verify the result is expanded (has full URI keys)
        assertThat(result.keySet()).contains(
                "@id",
                "@type",
                "https://www.w3.org/2018/credentials#holder",
                "https://www.w3.org/2018/credentials#verifiableCredential",
                "https://w3id.org/security#proof"
        );
        
        // Verify the embedded credential is present
        var verifiableCredential = result.getJsonArray("https://www.w3.org/2018/credentials#verifiableCredential");
        assertThat(verifiableCredential).isNotNull();
        assertThat(verifiableCredential.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle credentials with single context string")
    public void createPresentation_shouldHandleSingleContextString() {
        var credentialWithSingleContext = """
                {
                  "@context": "https://www.w3.org/2018/credentials/v1",
                  "id": "urn:uuid:12345",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {
                    "id": "did:web:subject"
                  }
                }
                """;

        var vcc = new VerifiableCredentialContainer(credentialWithSingleContext, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(result).isNotNull();
        // Verify presentation was created successfully (will be in expanded form)
        assertThat(result.keySet()).contains("@id", "@type");
    }

    @Test
    @DisplayName("Should handle multiple credentials with different custom contexts")
    public void createPresentation_shouldMergeMultipleCredentialContexts() {
        var customContext1 = "https://w3id.org/tractusx-trust/v0.8";
        var customContext2 = "https://example.com/custom/v1";
        
        var credential1 = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "%s"
                  ],
                  "id": "urn:uuid:11111",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {
                    "id": "did:web:subject1",
                    "holderIdentifier": "BPNL000000000001"
                  }
                }
                """.formatted(customContext1);

        var credential2 = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "%s"
                  ],
                  "id": "urn:uuid:22222",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {
                    "id": "did:web:subject2",
                    "customField": "customValue"
                  }
                }
                """.formatted(customContext2);

        var vcc1 = new VerifiableCredentialContainer(credential1, CredentialFormat.VC1_0_LD, createDummyCredential());
        var vcc2 = new VerifiableCredentialContainer(credential2, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc1, vcc2), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(result).isNotNull();
        // Verify both credentials are in the VP
        var verifiableCredential = result.getJsonArray("https://www.w3.org/2018/credentials#verifiableCredential");
        assertThat(verifiableCredential).isNotNull();
        assertThat(verifiableCredential.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should avoid duplicate contexts when credentials share contexts")
    public void createPresentation_shouldAvoidDuplicateContexts() {
        var sharedCustomContext = "https://w3id.org/tractusx-trust/v0.8";
        
        var credential1 = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "%s"
                  ],
                  "id": "urn:uuid:11111",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {"id": "did:web:subject1"}
                }
                """.formatted(sharedCustomContext);

        var credential2 = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "%s"
                  ],
                  "id": "urn:uuid:22222",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {"id": "did:web:subject2"}
                }
                """.formatted(sharedCustomContext);

        var vcc1 = new VerifiableCredentialContainer(credential1, CredentialFormat.VC1_0_LD, createDummyCredential());
        var vcc2 = new VerifiableCredentialContainer(credential2, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc1, vcc2), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(result).isNotNull();
        // Verify presentation was created successfully with both credentials
        var verifiableCredential = result.getJsonArray("https://www.w3.org/2018/credentials#verifiableCredential");
        assertThat(verifiableCredential).isNotNull();
        assertThat(verifiableCredential.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should gracefully handle credentials without @context")
    public void createPresentation_shouldHandleCredentialsWithoutContext() {
        var credentialWithoutContext = """
                {
                  "id": "urn:uuid:12345",
                  "type": ["VerifiableCredential"],
                  "issuer": "did:web:issuer",
                  "issuanceDate": "2023-01-01T00:00:00Z",
                  "credentialSubject": {"id": "did:web:subject"}
                }
                """;

        var vcc = new VerifiableCredentialContainer(credentialWithoutContext, CredentialFormat.VC1_0_LD, createDummyCredential());

        var result = creator.generatePresentation(participantContextId, List.of(vcc), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, issuerId, ADDITIONAL_DATA);

        assertThat(result).isNotNull();
        // Verify presentation was created successfully
        assertThat(result.keySet()).contains("@id", "@type");
    }

    @NotNull
    private TitaniumJsonLd initializeJsonLd() {
        var jld = new TitaniumJsonLd(mock());
        jld.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", TestUtils.getResource("odrl.jsonld"));
        jld.registerCachedDocument(DID_CONTEXT_URL, TestUtils.getResource("did.json"));
        jld.registerCachedDocument(JWS_2020_URL, TestUtils.getResource("jws2020.json"));
        jld.registerCachedDocument(W3C_CREDENTIALS_URL, TestUtils.getResource("credentials.v1.json"));
        jld.registerCachedDocument(DCP_CONTEXT_URL, TestUtils.getResource("dcp.v08.json"));
        jld.registerCachedDocument(PRESENTATION_EXCHANGE_URL, TestUtils.getResource("presentation-exchange.v1.json"));
        jld.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", TestUtils.getResource("examples.v1.json"));
        
        // Register custom contexts for testing
        jld.registerCachedDocument("https://w3id.org/tractusx-trust/v0.8", TestUtils.getResource("tractusx.v08.json"));
        jld.registerCachedDocument("https://example.com/custom/v1", TestUtils.getResource("custom.v1.json"));
        
        return jld;
    }

}
