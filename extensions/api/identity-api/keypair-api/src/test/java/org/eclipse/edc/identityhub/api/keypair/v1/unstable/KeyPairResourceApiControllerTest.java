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

package org.eclipse.edc.identityhub.api.keypair.v1.unstable;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.keypair.validation.KeyDescriptorValidator;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class KeyPairResourceApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private static final String PARTICIPANT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());

    private final KeyPairService keyPairService = mock();
    private final AuthorizationService authService = mock();

    @NotNull
    private static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .keyId("new-key-id")
                .privateKeyAlias("test-alias")
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"));
    }

    @BeforeEach
    void setUp() {
        when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void findById() {
        var keyPair = createKeyPair().build();

        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair)));

        var found = baseRequest()
                .get("/test-keypairId")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(KeyPairResource.class);
        assertThat(found).usingRecursiveComparison().isEqualTo(keyPair);
    }

    @Test
    void findById_notExist() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.notFound("tst-msg"));

        baseRequest()
                .get("/test-keypairId")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    @Test
    void findForParticipant() {
        var keyPair = createKeyPair().build();

        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair)));

        var found = baseRequest()
                .get("")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(KeyPairResource[].class);
        assertThat(found).usingRecursiveFieldByFieldElementComparator().containsExactly(keyPair);

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantContextId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals(PARTICIPANT_ID);
        }));
    }

    @Test
    void findForParticipant_noResult() {
        var keyPair = createKeyPair().build();

        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of()));

        var found = baseRequest()
                .get("")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(KeyPairResource[].class);
        assertThat(found).isEmpty();

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantContextId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals(PARTICIPANT_ID);
        }));
    }

    @Test
    void findForParticipant_notfound() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.notFound("test-message"));

        baseRequest()
                .get("")
                .then()
                .statusCode(404)
                .log().ifError();

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantContextId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals(PARTICIPANT_ID);
        }));
    }

    @ParameterizedTest(name = "Make default: {0}")
    @ValueSource(booleans = {true, false})
    void addKeyPair(boolean makeDefault) {
        var descriptor = createKeyDescriptor()
                .build();
        when(keyPairService.addKeyPair(eq(PARTICIPANT_ID), any(), eq(makeDefault))).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .put("?makeDefault=%s".formatted(makeDefault))
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).addKeyPair(eq(PARTICIPANT_ID), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())), eq(makeDefault));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void addKeyPair_invalidInput() {
        var descriptor = createKeyDescriptor()
                .privateKeyAlias(null)
                .build();

        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .put("?makeDefault=%s".formatted(true))
                .then()
                .log().ifError()
                .statusCode(400);

        verifyNoInteractions(keyPairService);
    }

    @Test
    void rotate() {
        var duration = Duration.ofDays(100).toMillis();
        when(keyPairService.rotateKeyPair(eq("old-id"), any(), eq(duration))).thenReturn(ServiceResult.success());

        var descriptor = createKeyDescriptor().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/rotate?duration=" + duration)
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).rotateKeyPair(eq("old-id"), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())), eq(duration));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void rotate_invalidInput() {
        var duration = Duration.ofDays(100).toMillis();

        var descriptor = createKeyDescriptor()
                .privateKeyAlias(null)
                .build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/rotate?duration=" + duration)
                .then()
                .log().ifError()
                .statusCode(400);

        verifyNoInteractions(keyPairService);
    }

    @Test
    void rotate_idNotFound() {
        var duration = Duration.ofDays(100).toMillis();
        when(keyPairService.rotateKeyPair(eq("old-id"), any(), eq(duration))).thenReturn(ServiceResult.notFound("test-message"));

        var descriptor = createKeyDescriptor().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/rotate?duration=" + duration)
                .then()
                .log().ifValidationFails()
                .statusCode(404);

        verify(keyPairService).rotateKeyPair(eq("old-id"), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())), eq(duration));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void rotate_withoutSuccessor() {
        var duration = Duration.ofDays(100).toMillis();
        when(keyPairService.rotateKeyPair(eq("old-id"), any(), eq(duration))).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .post("/old-id/rotate?duration=" + duration)
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).rotateKeyPair(eq("old-id"), isNull(), eq(duration));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void revoke() {
        when(keyPairService.revokeKey(eq("old-id"), any())).thenReturn(ServiceResult.success());

        var descriptor = createKeyDescriptor().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/revoke")
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).revokeKey(eq("old-id"), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void revoke_invalidInput() {

        var descriptor = createKeyDescriptor()
                .privateKeyAlias(null)
                .build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/revoke")
                .then()
                .log().ifError()
                .statusCode(400);

        verifyNoInteractions(keyPairService);
    }

    @Test
    void revoke_notFound() {
        when(keyPairService.revokeKey(eq("old-id"), any())).thenReturn(ServiceResult.notFound("test-message"));

        var descriptor = createKeyDescriptor().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .post("/old-id/revoke")
                .then()
                .log().ifError()
                .statusCode(404);

        verify(keyPairService).revokeKey(eq("old-id"), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void activate() {
        var id = "keypair-id";
        when(keyPairService.activate(eq(id))).thenReturn(ServiceResult.success());
        baseRequest()
                .contentType(ContentType.JSON)
                .post("/%s/activate".formatted(id))
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).activate(eq(id));
        verifyNoMoreInteractions(keyPairService);
    }

    @Test
    void actvate_whenNotAllowed() {
        var id = "keypair-id";
        when(keyPairService.activate(eq(id))).thenReturn(ServiceResult.badRequest("foo-bar"));
        baseRequest()
                .contentType(ContentType.JSON)
                .post("/%s/activate".formatted(id))
                .then()
                .log().ifError()
                .statusCode(400);

        verify(keyPairService).activate(eq(id));
        verifyNoMoreInteractions(keyPairService);
    }

    @Override
    protected Object controller() {
        return new KeyPairResourceApiController(authService, keyPairService, new KeyDescriptorValidator(mock()));
    }

    private KeyPairResource.Builder createKeyPair() {
        return KeyPairResource.Builder.newInstance()
                .id("test-keypair")
                .participantContextId(PARTICIPANT_ID)
                .isDefaultPair(true)
                .privateKeyAlias("test-alias")
                .useDuration(Duration.ofDays(365).toMillis());
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/%s/keypairs".formatted(PARTICIPANT_ID_ENCODED))
                .when();
    }
}