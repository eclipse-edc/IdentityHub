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

import com.nimbusds.jose.JOSEException;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.TestData;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
public class PresentationApiComponentTest {

    protected static final IdentityHubRuntimeConfiguration IDENTITY_HUB_PARTICIPANT = IdentityHubRuntimeConfiguration.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    private static final String VALID_QUERY_WITH_SCOPE = """
            {
              "@context": [
                "https://identity.foundation/presentation-exchange/submission/v1",
                "https://w3id.org/tractusx-trust/v0.8"
              ],
              "@type": "PresentationQueryMessage",
              "scope":[
                "org.eclipse.edc.vc.type:TestScope1:read"
              ]
            }
            """;
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "did:web:consumer";
    private static final String TEST_PARTICIPANT_CONTEXT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(TEST_PARTICIPANT_CONTEXT_ID.getBytes());
    private static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();

    @RegisterExtension
    static EdcRuntimeExtension runtime;

    static {
        runtime = new EdcRuntimeExtension(":launcher", "identity-hub", IDENTITY_HUB_PARTICIPANT.controlPlaneConfiguration());
        runtime.registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
    }


    @Test
    void query_tokenNotPresent_shouldReturn401() {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType("application/json")
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(401)
                .extract().body().asString();
    }

    @Test
    void query_validationError_shouldReturn400() {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
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
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
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
    void query_tokenVerificationFails_shouldReturn401() {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
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
                .body("[0].message", equalTo("ID token verification failed: token not verified"));
    }

    @Test
    void query_queryResolutionFails_shouldReturn403() {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
        var token = generateSiToken();

        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                .then()
                .statusCode(403)
                .log().ifValidationFails()
                .body("[0].type", equalTo("NotAuthorized"))
                .body("[0].message", equalTo("scope mismatch!"));
    }

    @Test
    void query_presentationGenerationFails_shouldReturn500() {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
        var token = generateSiToken();

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
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
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
                .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(2));

    }

    @Test
    void query_success() throws JOSEException {
        createParticipant(TEST_PARTICIPANT_CONTEXT_ID);
        var store = runtime.getContext().getService(CredentialStore.class);
        //todo: store credential 
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
                .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(2));

    }

    private PresentationResponseMessage createPresentationResponse() {
        var submission = new PresentationSubmission("id", "def-id", List.of(new InputDescriptorMapping("input-id", "ldp-vp", "foo")));
        return PresentationResponseMessage.Builder.newinstance()
                .presentation(List.of(TestData.VC_EXAMPLE))
                .presentationSubmission(submission)
                .build();
    }

    private void createParticipant(String participantId) {
        var service = runtime.getContext().getService(ParticipantContextService.class);
        var vault = runtime.getContext().getService(Vault.class);

        var key = JwtCreationUtil.CONSUMER_KEY;
        vault.storeSecret(key.getKeyID(), key.toPublicJWK().toJSONString());

        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .did("did:web:%s".formatted(participantId))
                .active(true)
                .key(KeyDescriptor.Builder.newInstance()
                        .publicKeyJwk(key.toPublicJWK().toJSONObject())
                        .privateKeyAlias("%s-privatekey-alias".formatted(participantId))
                        .keyId("key-1")
                        .build())
                .build();
        service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

}
