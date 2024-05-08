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

package org.eclipse.edc.identityhub.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.TestData;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EndToEndTest
public class PresentationApiEndToEndTest {

    protected static final IdentityHubRuntimeConfiguration IDENTITY_HUB_PARTICIPANT = IdentityHubRuntimeConfiguration.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    private static final String KEY_RESOURCE_ID = "key-1";
    private static final String VALID_QUERY_WITH_SCOPE = """
            {
              "@context": [
                "https://identity.foundation/presentation-exchange/submission/v1",
                "https://w3id.org/tractusx-trust/v0.8"
              ],
              "@type": "PresentationQueryMessage",
              "scope":[
                "org.eclipse.edc.vc.type:AlumniCredential:read"
              ]
            }
            """;
    private static final String VALID_QUERY_WITH_ADDITIONAL_SCOPE = """
            {
              "@context": [
                "https://identity.foundation/presentation-exchange/submission/v1",
                "https://w3id.org/tractusx-trust/v0.8"
              ],
              "@type": "PresentationQueryMessage",
              "scope":[
                "org.eclipse.edc.vc.type:AlumniCredential:read",
                "org.eclipse.edc.vc.type:SuperSecretCredential:*"
              ]
            }
            """;
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "did:web:consumer";
    private static final String TEST_PARTICIPANT_CONTEXT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(TEST_PARTICIPANT_CONTEXT_ID.getBytes());
    private static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
    private static final RevocationListService REVOCATION_LIST_SERVICE = mock();
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    @RegisterExtension
    static EdcRuntimeExtension runtime;

    static {
        runtime = new EdcRuntimeExtension(":launcher", "identity-hub", IDENTITY_HUB_PARTICIPANT.controlPlaneConfiguration());
        runtime.registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
        runtime.registerServiceMock(RevocationListService.class, REVOCATION_LIST_SERVICE);
    }

    @Test
    void query_tokenNotPresent_shouldReturn401() {
        createParticipant();
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType("application/json")
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(401)
                .extract().body().asString();
    }

    @Test
    void query_validationError_shouldReturn400() {
        createParticipant();
        var query = """
                {
                  "@context": [
                    "https://identity.foundation/participants/test-participant/presentation-exchange/submission/v1",
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationQueryMessage"
                }
                """;
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, generateSiToken())
                .body(query)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(400)
                .extract().body().asString();

    }

    @Test
    void query_withPresentationDefinition_shouldReturn503() {
        createParticipant();
        var query = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationQueryMessage",
                  "presentationDefinition":{
                  }
                }
                """;
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, generateSiToken())
                .body(query)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(503)
                .extract().body().asString();
    }

    @Test
    void query_tokenVerificationFails_shouldReturn401() throws JOSEException {
        createParticipant();

        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:provider#key1").generate();
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(spoofedKey.toPublicKey()));

        var token = generateSiToken();
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(401)
                .log().ifValidationFails()
                .body("[0].type", equalTo("AuthenticationFailed"))
                .body("[0].message", equalTo("ID token verification failed: Token verification failed"));
    }

    @Test
    void query_credentialQueryResolverFails_shouldReturn403() throws JOSEException, JsonProcessingException {
        createParticipant();
        var token = generateSiToken();

        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(JwtCreationUtil.CONSUMER_KEY.toPublicKey()));
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(JwtCreationUtil.PROVIDER_KEY.toPublicKey()));

        // create the credential in the store
        var store = runtime.getContext().getService(CredentialStore.class);
        var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
        var res = VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.JWT, cred))
                .issuerId("https://example.edu/issuers/565049")
                .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .build();
        store.create(res);

        // create another credential in the store
        var cred2 = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE_2, VerifiableCredential.class);
        var res2 = VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE_2, CredentialFormat.JWT, cred2))
                .issuerId("https://example.edu/issuers/12345")
                .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .build();
        store.create(res2);


        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_ADDITIONAL_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .log().ifError()
                .statusCode(403)
                .body("[0].type", equalTo("NotAuthorized"))
                .body("[0].message", equalTo("Invalid query: requested Credentials outside of scope."));
    }

    @Test
    void query_presentationGenerationFails_shouldReturn500() {
        createParticipant();
        var token = generateSiToken();

        // delete the key, so the presentation generation will fail
        var kpStore = runtime.getService(KeyPairResourceStore.class);
        kpStore.deleteById(KEY_RESOURCE_ID)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(500)
                .log().ifValidationFails();
    }

    @Test
    void query_success_noCredentials() throws JOSEException {
        createParticipant();
        var token = generateSiToken();

        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(JwtCreationUtil.CONSUMER_KEY.toPublicKey()));
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(JwtCreationUtil.PROVIDER_KEY.toPublicKey()));

        var response = IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(JsonObject.class);

        assertThat(response)
                .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(2))
                .hasEntrySatisfying("presentation", jsonValue -> assertThat(extractCredentials(((JsonString) jsonValue).getString())).isEmpty());

    }

    @Test
    void query_success_containsCredential() throws JOSEException, JsonProcessingException {
        createParticipant();
        var store = runtime.getContext().getService(CredentialStore.class);
        var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
        var res = VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.JWT, cred))
                .issuerId("https://example.edu/issuers/565049")
                .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .build();

        store.create(res);
        var token = generateSiToken();
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(JwtCreationUtil.CONSUMER_KEY.toPublicKey()));
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(JwtCreationUtil.PROVIDER_KEY.toPublicKey()));

        var response = IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(JsonObject.class);

        assertThat(response)
                .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(2))
                .hasEntrySatisfying("presentation", jsonValue -> {
                    assertThat(jsonValue.getValueType()).isEqualTo(JsonValue.ValueType.STRING);
                    var vpToken = ((JsonString) jsonValue).getString();
                    assertThat(vpToken).isNotNull();
                    assertThat(extractCredentials(vpToken)).isNotEmpty();
                });

    }


    @ParameterizedTest(name = "VcState code: {0}")
    @ValueSource(ints = { 600, 700, 800, 900 })
    void query_shouldFilterOutInvalidCreds(int vcStateCode) throws JOSEException, JsonProcessingException {
        createParticipant();
        var store = runtime.getContext().getService(CredentialStore.class);

        // modify VC content, so that it becomes either not-yet-valid or expired
        var vcContent = TestData.VC_EXAMPLE;
        if (vcStateCode == VcStatus.EXPIRED.code()) {
            var expirationInPast = Instant.now().minus(1, ChronoUnit.DAYS).toString();
            vcContent = vcContent.replaceAll("\"expirationDate\": \"2999-01-01T19:23:24Z\",", "\"expirationDate\": \"" + expirationInPast + "\",");
        } else if (vcStateCode == VcStatus.NOT_YET_VALID.code()) {
            var futureIssuance = Instant.now().plus(1, ChronoUnit.DAYS).toString();
            vcContent = vcContent.replaceAll("\"issuanceDate\": \".*\",", "\"issuanceDate\": \"" + futureIssuance + "\",");
        }


        var cred = OBJECT_MAPPER.readValue(vcContent, VerifiableCredential.class);
        // inject a CredentialStatus object, that triggers the revocation check
        if (vcStateCode == VcStatus.SUSPENDED.code()) {
            cred.getCredentialStatus().add(new CredentialStatus("test-cred-stat-id", "StatusList2021Entry",
                    Map.of("statusListCredential", "https://university.example/credentials/status/3",
                            "statusPurpose", "suspension",
                            "statusListIndex", 69)));
            when(REVOCATION_LIST_SERVICE.checkValidity(any(VerifiableCredential.class)))
                    .thenReturn(Result.failure("suspended"));
        }
        // create the credential in the store
        var res = VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.from(vcStateCode))
                .credential(new VerifiableCredentialContainer(vcContent, CredentialFormat.JWT, cred))
                .issuerId("https://example.edu/issuers/565049")
                .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .build();
        store.create(res);

        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(JwtCreationUtil.CONSUMER_KEY.toPublicKey()));
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(JwtCreationUtil.PROVIDER_KEY.toPublicKey()));

        var token = generateSiToken();
        var response = IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(JsonObject.class);

        assertThat(response)
                .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(2))
                .hasEntrySatisfying("presentation", jsonValue -> {
                    assertThat(jsonValue.getValueType()).isEqualTo(JsonValue.ValueType.STRING);
                    var vpToken = ((JsonString) jsonValue).getString();
                    assertThat(vpToken).isNotNull();
                    assertThat(extractCredentials(vpToken)).isEmpty(); // credential should be filtered out
                });

    }

    /**
     * extracts a (potentially empty) list of verifiable credentials from a JWT-VP
     */
    @SuppressWarnings("unchecked")
    private List<VerifiableCredential> extractCredentials(String vpToken) {
        try {
            var jwt = SignedJWT.parse(vpToken);
            var vpClaim = jwt.getJWTClaimsSet().getClaim("vp");
            if (vpClaim == null) return List.of();

            Map<String, Object> map = (Map<String, Object>) OBJECT_MAPPER.convertValue(vpClaim, Map.class);

            return (List<VerifiableCredential>) map.get("verifiableCredential");

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private PresentationResponseMessage createPresentationResponse() {
        var submission = new PresentationSubmission("id", "def-id", List.of(new InputDescriptorMapping("input-id", "ldp-vp", "foo")));
        return PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of(TestData.VC_EXAMPLE))
                .presentationSubmission(submission)
                .build();
    }

    private void createParticipant() {
        var service = runtime.getContext().getService(ParticipantContextService.class);
        var vault = runtime.getContext().getService(Vault.class);

        var key = JwtCreationUtil.CONSUMER_KEY;
        var privateKeyAlias = "%s-privatekey-alias".formatted(PresentationApiEndToEndTest.TEST_PARTICIPANT_CONTEXT_ID);
        vault.storeSecret(key.getKeyID(), key.toPublicJWK().toJSONString());
        vault.storeSecret(privateKeyAlias, key.toJSONString());
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(PresentationApiEndToEndTest.TEST_PARTICIPANT_CONTEXT_ID)
                .did("did:web:%s".formatted(PresentationApiEndToEndTest.TEST_PARTICIPANT_CONTEXT_ID))
                .active(true)
                .key(KeyDescriptor.Builder.newInstance()
                        .publicKeyJwk(key.toPublicJWK().toJSONObject())
                        .privateKeyAlias(privateKeyAlias)
                        .keyId(KEY_RESOURCE_ID)
                        .build())
                .build();
        service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

}
