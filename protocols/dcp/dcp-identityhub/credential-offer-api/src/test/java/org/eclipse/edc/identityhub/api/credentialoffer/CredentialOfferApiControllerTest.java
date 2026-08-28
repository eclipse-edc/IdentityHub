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

package org.eclipse.edc.identityhub.api.credentialoffer;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class CredentialOfferApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DcpIssuerTokenVerifier tokenVerifier = mock();
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final CredentialOfferService offerService = mock();
    private final CredentialObjectResolver credentialObjectResolver = mock();
    private final CredentialOfferApiController controller = new CredentialOfferApiController(validatorRegistry, typeTransformerRegistry, tokenVerifier, participantContextService, offerService, new TitaniumJsonLd(mock()), credentialObjectResolver);

    @BeforeEach
    void setUp() {
        when(validatorRegistry.validate(anyString(), any())).thenReturn(ValidationResult.success());

        when(credentialObjectResolver.resolve(any(), any())).thenAnswer(i -> Result.success(i.getArgument(1)));

        when(tokenVerifier.verify(any(), anyString())).thenReturn(Result.success(
                ClaimToken.Builder.newInstance()
                        .claim("foo", "bar")
                        .build()
        ));

        when(typeTransformerRegistry.forContext(anyString())).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.forContext(anyString())).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialOfferMessage.class)))
                .thenReturn(Result.success(CredentialOfferMessage.Builder.newInstance().issuer("test-issuer").build()));

        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.success(
                IdentityHubParticipantContext.Builder.newInstance()
                        .participantContextId("test-id")
                        .did("did:web:test-id")
                        .state(ParticipantContextState.CREATED)
                        .apiTokenAlias("test-alias")
                        .build()
        ));

        when(offerService.create(any())).thenReturn(ServiceResult.success());
    }

    @Test
    void offerCredential_success() {
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(204);
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

    @Test
    void offerCredential_storageFails_expect40x() {
        when(offerService.create(any())).thenReturn(ServiceResult.conflict("foobar"));
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(409);
    }

    // A4.7: token-verification failure must return the same status code on both DCP endpoints — aligned on 401 (the Storage API already returns 401; this endpoint currently returns 403)
    @Test
    @DisplayName("A4.7: token verification failure returns 401, consistent with the Storage API")
    void offerCredential_invalidAuthToken_statusCode401() {
        when(tokenVerifier.verify(any(), anyString())).thenReturn(Result.failure("foobar"));

        // 401 is the agreed code for token-verification failures on both DCP endpoints
        baseRequest()
                .body(createRequestBody())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
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
                        .add(toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
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