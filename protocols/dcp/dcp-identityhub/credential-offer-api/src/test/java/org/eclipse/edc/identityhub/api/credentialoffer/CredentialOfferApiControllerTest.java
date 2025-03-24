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

package org.eclipse.edc.identityhub.api.credentialoffer;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILES_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredentialOfferApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DcpIssuerTokenVerifier tokenVerifier = mock();
    private final ParticipantContextService participantContextService = mock();
    private final CredentialOfferApiController controller = new CredentialOfferApiController(validatorRegistry, typeTransformerRegistry, mock(), tokenVerifier, participantContextService);

    @BeforeEach
    void setUp() {
        when(validatorRegistry.validate(anyString(), any())).thenReturn(ValidationResult.success());

        when(tokenVerifier.verify(any(), anyString())).thenReturn(Result.success(
                ClaimToken.Builder.newInstance()
                        .claim("foo", "bar")
                        .build()
        ));

        when(typeTransformerRegistry.forContext(anyString())).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.forContext(anyString())).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialOfferMessage.class)))
                .thenReturn(Result.success(CredentialOfferMessage.Builder.newInstance().build()));

        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.success(
                ParticipantContext.Builder.newInstance()
                        .participantContextId("test-id")
                        .did("did:web:test-id")
                        .state(ParticipantContextState.CREATED)
                        .apiTokenAlias("test-alias")
                        .build()
        ));
    }

    @Test
    void offerCredential_success() {
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    @Test
    void offerCredential_missingRequestBody_expect400() {
        baseRequest()
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    @Test
    void offerCredential_invalidRequest_expect400() {
        when(validatorRegistry.validate(anyString(), any())).thenReturn(ValidationResult.failure(Violation.violation("foo", null)));
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    @Test
    void offerCredential_invalidAuthToken_expect403() {
        when(tokenVerifier.verify(any(), anyString())).thenReturn(Result.failure("foobar"));
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void offerCredential_missingAuthHeader_expect401() {
        given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/participants/" + PARTICIPANT_ID + "/offers")
                //missing: Auth header
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void offerCredential_missingParticipantContextId_expect401() {
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.notFound("foobar"));
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void offerCredential_transformationFailed_expect400() {
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialOfferMessage.class)))
                .thenReturn(Result.failure("foobar"));
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    @Override
    protected Object controller() {
        return controller;
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/participants/" + PARTICIPANT_ID + "/offers")
                .header("Authorization", "Bearer test-token")
                .when();
    }

    private JsonObject createRequestBody() {
        var credentialsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(toIri(CREDENTIAL_OBJECT_PROFILES_TERM), Json.createArrayBuilder(List.of("profile")))
                        .add(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "offerReason")
                        .add(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                        .add(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("binding")))
                        .build());
        return Json.createObjectBuilder()
                .add(toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(toIri(CREDENTIALS_TERM), credentialsArray)
                .build();
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}