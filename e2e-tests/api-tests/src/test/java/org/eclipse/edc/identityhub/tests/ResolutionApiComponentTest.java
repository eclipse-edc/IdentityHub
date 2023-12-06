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

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.edc.identityhub.spi.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.TestData;
import org.eclipse.edc.identitytrust.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationResponse;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
public class ResolutionApiComponentTest {
    public static final String VALID_QUERY_WITH_SCOPE = """
            {
              "@context": [
                "https://identity.foundation/presentation-exchange/submission/v1",
                "https://w3id.org/tractusx-trust/v0.8"
              ],
              "@type": "Query",
              "scope":[
                "test-scope1"
              ]
            }
            """;
    protected static final IdentityHubRuntimeConfiguration IDENTITY_HUB_PARTICIPANT = IdentityHubRuntimeConfiguration.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    // todo: these mocks should be replaced, once their respective implementations exist!
    private static final CredentialQueryResolver CREDENTIAL_QUERY_RESOLVER = mock();
    private static final VerifiablePresentationService PRESENTATION_GENERATOR = mock();
    private static final AccessTokenVerifier ACCESS_TOKEN_VERIFIER = mock();

    @RegisterExtension
    static EdcRuntimeExtension runtime;

    static {
        runtime = new EdcRuntimeExtension(":launcher", "identity-hub", IDENTITY_HUB_PARTICIPANT.controlPlaneConfiguration());
        runtime.registerServiceMock(CredentialQueryResolver.class, CREDENTIAL_QUERY_RESOLVER);
        runtime.registerServiceMock(VerifiablePresentationService.class, PRESENTATION_GENERATOR);
        runtime.registerServiceMock(AccessTokenVerifier.class, ACCESS_TOKEN_VERIFIER);
    }

    private final ECKey consumerKey = generateEcKey();
    private final ECKey providerKey = generateEcKey();

    @Test
    void query_tokenNotPresent_shouldReturn401() {
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType("application/json")
                .post("/presentation/query")
                .then()
                .statusCode(401)
                .extract().body().asString();
    }

    @Test
    void query_validationError_shouldReturn400() {
        var query = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "Query"
                }
                """;
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, generateSiToken())
                .body(query)
                .post("/presentation/query")
                .then()
                .statusCode(400)
                .extract().body().asString();

    }

    @Test
    void query_withPresentationDefinition_shouldReturn503() {
        var query = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "Query",
                  "presentationDefinition":{
                  }
                }
                """;
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, generateSiToken())
                .body(query)
                .post("/presentation/query")
                .then()
                .statusCode(503)
                .extract().body().asString();
    }


    @Test
    void query_tokenVerificationFails_shouldReturn401() {
        var token = generateSiToken();
        when(ACCESS_TOKEN_VERIFIER.verify(eq(token))).thenReturn(failure("token not verified"));
        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/presentation/query")
                .then()
                .statusCode(401)
                .log().ifValidationFails()
                .body("[0].type", equalTo("AuthenticationFailed"))
                .body("[0].message", equalTo("ID token verification failed: token not verified"));
    }

    @Test
    void query_queryResolutionFails_shouldReturn403() {
        var token = generateSiToken();
        when(ACCESS_TOKEN_VERIFIER.verify(eq(token))).thenReturn(success(List.of("test-scope1")));
        when(CREDENTIAL_QUERY_RESOLVER.query(any(), ArgumentMatchers.anyList())).thenReturn(QueryResult.unauthorized("scope mismatch!"));

        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/presentation/query")
                .then()
                .statusCode(403)
                .log().ifValidationFails()
                .body("[0].type", equalTo("NotAuthorized"))
                .body("[0].message", equalTo("scope mismatch!"));
    }

    @Test
    void query_presentationGenerationFails_shouldReturn500() {
        var token = generateSiToken();
        when(ACCESS_TOKEN_VERIFIER.verify(eq(token))).thenReturn(success(List.of("test-scope1")));
        when(CREDENTIAL_QUERY_RESOLVER.query(any(), ArgumentMatchers.anyList())).thenReturn(QueryResult.success(Stream.empty()));
        when(PRESENTATION_GENERATOR.createPresentation(anyList(), eq(null), any())).thenReturn(failure("generator test error"));

        IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/presentation/query")
                .then()
                .statusCode(500)
                .log().ifValidationFails();
    }

    @Test
    void query_success() {
        var token = generateSiToken();
        when(ACCESS_TOKEN_VERIFIER.verify(eq(token))).thenReturn(success(List.of("test-scope1")));
        when(CREDENTIAL_QUERY_RESOLVER.query(any(), ArgumentMatchers.anyList())).thenReturn(QueryResult.success(Stream.empty()));
        when(PRESENTATION_GENERATOR.createPresentation(anyList(), eq(null), any())).thenReturn(success(createPresentationResponse()));

        var resp = IDENTITY_HUB_PARTICIPANT.getResolutionEndpoint().baseRequest()
                .contentType(JSON)
                .header(AUTHORIZATION, token)
                .body(VALID_QUERY_WITH_SCOPE)
                .post("/presentation/query")
                .then()
                .statusCode(200)
                .log().ifValidationFails()
                .extract().body().as(PresentationResponse.class);

    }

    private PresentationResponse createPresentationResponse() {
        var submission = new PresentationSubmission("id", "def-id", List.of(new InputDescriptorMapping("input-id", "ldp-vp", "foo")));
        return new PresentationResponse(new Object[] {TestData.VP_EXAMPLE}, submission);
    }


}
