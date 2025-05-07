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

package org.eclipse.edc.identityhub.api.storage;

import com.nimbusds.jwt.JWTClaimsSet;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialContainer;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class StorageApiControllerTest extends RestControllerTestBase {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final Monitor monitor = mock();
    private final CredentialWriter credentialWriter = mock();
    private final DcpIssuerTokenVerifier issuerTokenVerifier = mock();
    private final ParticipantContextService participantContextService = mock();

    @BeforeEach
    void setUp() {
        when(transformerRegistry.forContext(eq(DCP_SCOPE_V_1_0))).thenReturn(transformerRegistry);
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CredentialMessage.class)))
                .thenReturn(Result.success(credentialMessage()));

        when(issuerTokenVerifier.verify(any(), anyString())).thenReturn(Result.success(claimToken()));
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.success(participantContext()));
        when(credentialWriter.write(anyString(), anyString(), anyCollection(), anyString())).thenReturn(ServiceResult.success());
    }

    @Test
    void storeCredential_success_expect200() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    @Test
    void storeCredential_tokenNotPresent_shouldReturn401() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        baseRequest()
                // missing: auth header
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
        verifyNoMoreInteractions(issuerTokenVerifier, validatorRegistry, transformerRegistry);
    }

    @Test
    void storeCredential_validationError_shouldReturn400() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("foo", null)));
        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
        verifyNoInteractions(issuerTokenVerifier, transformerRegistry);
    }

    @Test
    void storeCredential_transformationError_shouldReturn400() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CredentialMessage.class))).thenReturn(Result.failure("foobar"));
        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
        verifyNoMoreInteractions(issuerTokenVerifier);
    }

    @Test
    void storeCredential_participantNotFound_shouldReturn401() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.notFound("foo"));

        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void storeCredential_tokenValidationFails_shouldReturn401() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(issuerTokenVerifier.verify(any(), anyString())).thenReturn(Result.failure("foo"));
        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    @Disabled
    void storeCredential_whenWriteFails_shouldReturn500() {
        // todo: add mock for credential storing service
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(500);
    }

    @Test
    void storeCredential_writerFails_shouldReturn400() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(credentialWriter.write(anyString(), anyString(), anyCollection(), anyString())).thenReturn(ServiceResult.badRequest("foo"));

        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(containsString("foo"));
    }

    @Test
    void storeCredential_writerReturnsNotAuthorized_shouldReturn403() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(credentialWriter.write(anyString(), anyString(), anyCollection(), anyString())).thenReturn(ServiceResult.unauthorized("foo"));

        baseRequest()
                .header("Authorization", "Bearer " + generateJwt())
                .body(credentialMessageJson())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(containsString("foo"));
    }

    @Override
    protected Object controller() {
        return new StorageApiController(validatorRegistry,
                transformerRegistry,
                new TitaniumJsonLd(monitor),
                credentialWriter,
                mock(),
                issuerTokenVerifier,
                participantContextService
        );
    }

    private ClaimToken claimToken() {
        return ClaimToken.Builder.newInstance().build();
    }

    private ParticipantContext participantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId("participantId")
                .did("did:web:test")
                .apiTokenAlias("alias")
                .build();
    }

    private CredentialMessage credentialMessage() {
        return CredentialMessage.Builder.newInstance()
                .issuerPid(UUID.randomUUID().toString())
                .holderPid(UUID.randomUUID().toString())
                .status("ISSUED")
                .credential(new CredentialContainer("SomeCredential", "vcdm11_jwt", "SOME_JWT_STRING"))
                .build();
    }

    private JsonObject credentialMessageJson() {
        return Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("status"), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("requestId"), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentials"), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentialType"), "SomeCredential")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("format"), "vcdm11_jwt")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("payload"), "SOME_JWT_STRING")))
                .build();
    }

    private RequestSpecification baseRequest() {
        var s = Base64.getUrlEncoder().encodeToString("test-participant".getBytes());

        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/participants/" + s + "/credentials")
                .when();
    }

    private String generateJwt() {
        var ecKey = generateEcKey(null);
        var jwt = buildSignedJwt(new JWTClaimsSet.Builder().audience("test-audience")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issuer("test-issuer")
                .subject("test-subject")
                .jwtID(UUID.randomUUID().toString()).build(), ecKey);

        return jwt.serialize();
    }

}