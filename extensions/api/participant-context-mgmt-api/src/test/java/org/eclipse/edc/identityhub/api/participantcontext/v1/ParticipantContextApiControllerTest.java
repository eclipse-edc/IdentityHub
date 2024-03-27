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

package org.eclipse.edc.identityhub.api.participantcontext.v1;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.api.v1.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class ParticipantContextApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private static final String PARTICIPANT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());

    private final ParticipantContextService participantContextServiceMock = mock();
    private final AuthorizationService authService = mock();
    private final ParticipantManifestValidator participantManifestValidator = mock();

    @BeforeEach
    void setUp() {
        when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void getById() {
        var pc = createParticipantContext().build();
        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.success(pc));

        var participantContext = baseRequest()
                .get("/%s".formatted(pc.getParticipantId()))
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(ParticipantContext.class);

        assertThat(participantContext).usingRecursiveComparison().isEqualTo(pc);
        verify(participantContextServiceMock).getParticipantContext(any());
    }

    @Test
    void getById_whenNotFound() {
        when(participantContextServiceMock.getParticipantContext(any())).thenReturn(ServiceResult.notFound("foo bar"));

        baseRequest()
                .get("/unknown")
                .then()
                .statusCode(404)
                .log().ifError();
    }

    @Test
    void createParticipant_success() {
        when(participantContextServiceMock.createParticipantContext(any())).thenReturn(ServiceResult.success());
        when(participantManifestValidator.validate(any())).thenReturn(ValidationResult.success());
        var manifest = createManifest().build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(manifest)
                .post()
                .then()
                .statusCode(204);
        verify(participantContextServiceMock).createParticipantContext(any(ParticipantManifest.class));
    }

    @Test
    void createParticipant_invalidManifest() {
        var manifest = createManifest()
                .participantId(null)
                .build();
        when(participantManifestValidator.validate(any())).thenReturn(ValidationResult.failure(emptyList()));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(manifest)
                .post()
                .then()
                .statusCode(400);

        verifyNoInteractions(participantContextServiceMock);
    }

    @Test
    void createParticipant_invalidKeyDescriptor() {
        var manifest = createManifest()
                .key(createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(null).build())
                .build();
        when(participantManifestValidator.validate(any())).thenReturn(ValidationResult.failure(emptyList()));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(manifest)
                .post()
                .then()
                .statusCode(400);

        verifyNoInteractions(participantContextServiceMock);
    }

    @Test
    void createParticipant_alreadyExists() {
        when(participantContextServiceMock.createParticipantContext(any())).thenReturn(ServiceResult.conflict("already exists"));
        var manifest = createManifest().build();
        when(participantManifestValidator.validate(any())).thenReturn(ValidationResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body(manifest)
                .post()
                .then()
                .statusCode(409);

        verify(participantContextServiceMock).createParticipantContext(any(ParticipantManifest.class));
    }

    @Test
    void regenerateToken() {
        when(participantContextServiceMock.regenerateApiToken(any())).thenReturn(ServiceResult.success("new-api-token"));
        baseRequest()
                .post("/%s/token".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(200)
                .body(equalTo("new-api-token"));
        verify(participantContextServiceMock).regenerateApiToken(PARTICIPANT_ID);
    }

    @Test
    void regenerateToken_notFound() {
        when(participantContextServiceMock.regenerateApiToken(any())).thenReturn(ServiceResult.notFound("foo-bar"));
        baseRequest()
                .post("/%s/token".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(404);
        verify(participantContextServiceMock).regenerateApiToken(PARTICIPANT_ID);
    }

    @Test
    void activateParticipant() {
        when(participantContextServiceMock.updateParticipant(any(), any())).thenReturn(ServiceResult.success());
        baseRequest()
                .post("/%s/state?isActive=true".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(204);
        verify(participantContextServiceMock).updateParticipant(eq(PARTICIPANT_ID), any());
    }

    @Test
    void deactivateParticipant() {
        when(participantContextServiceMock.updateParticipant(any(), any())).thenReturn(ServiceResult.success());
        baseRequest()
                .post("/%s/state?isActive=false".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(204);
        verify(participantContextServiceMock).updateParticipant(eq(PARTICIPANT_ID), any());
    }

    @Test
    void delete() {
        when(participantContextServiceMock.deleteParticipantContext(any())).thenReturn(ServiceResult.success());
        baseRequest()
                .delete("/%s".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(204);
        verify(participantContextServiceMock).deleteParticipantContext(PARTICIPANT_ID);
    }

    @Test
    void delete_notFound() {
        when(participantContextServiceMock.deleteParticipantContext(any())).thenReturn(ServiceResult.notFound("foo bar"));
        baseRequest()
                .delete("/%s".formatted(PARTICIPANT_ID_ENCODED))
                .then()
                .statusCode(404);
        verify(participantContextServiceMock).deleteParticipantContext(PARTICIPANT_ID);
    }

    @Test
    void updateRoles() {
        when(participantContextServiceMock.updateParticipant(anyString(), any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(List.of("role1", "role2"))
                .put("/test-participant/roles")
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        verify(participantContextServiceMock).updateParticipant(anyString(), argThat(con -> {
            var pc = createParticipantContext().build();
            con.accept(pc);
            return pc.getRoles().containsAll(List.of("role1", "role2"));
        }));

    }

    @Test
    void updateRoles_notFound() {
        when(participantContextServiceMock.updateParticipant(anyString(), any())).thenReturn(ServiceResult.notFound("foobar"));

        baseRequest()
                .body(List.of("role1", "role2"))
                .put("/test-participant/roles")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
        verify(participantContextServiceMock).updateParticipant(anyString(), any());
    }

    @Test
    void getAll() {
        var list = IntStream.range(0, 10).mapToObj(i -> createParticipantContext().participantId("participant" + i).build()).toList();
        when(participantContextServiceMock.query(any())).thenReturn(ServiceResult.success(list));

        var participantContexts = baseRequest()
                .get("/")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(ParticipantContext[].class);

        assertThat(participantContexts).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(list);
        verify(participantContextServiceMock).query(any());
    }

    @Override
    protected Object controller() {
        return new ParticipantContextApiController(participantManifestValidator, participantContextServiceMock, authService);
    }

    private ParticipantContext.Builder createParticipantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId("test-id")
                .createdAt(Instant.now().toEpochMilli())
                .state(ParticipantContextState.ACTIVATED)
                .apiTokenAlias("test-alias");
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/participants")
                .when();
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .key(createKey().build())
                .active(true)
                .participantId("test-id")
                .did("did:web:test-id");
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance().keyId("test-kie")
                .privateKeyAlias("private-alias")
                .publicKeyJwk(createJwk());
    }

    private Map<String, Object> createJwk() {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .generate()
                    .toJSONObject();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}