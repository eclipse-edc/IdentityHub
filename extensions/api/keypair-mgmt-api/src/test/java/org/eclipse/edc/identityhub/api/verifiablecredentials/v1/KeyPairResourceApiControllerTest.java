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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KeyPairResourceApiControllerTest extends RestControllerTestBase {

    private final KeyPairService keyPairService = mock();
    private final AuthorizationService authService= mock();


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

        var found = baseRequest()
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
                .get("?participantId=test-participant")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(KeyPairResource[].class);
        assertThat(found).usingRecursiveFieldByFieldElementComparator().containsExactly(keyPair);

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals("test-participant");
        }));
    }

    @Test
    void findForParticipant_noResult() {
        var keyPair = createKeyPair().build();

        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of()));

        var found = baseRequest()
                .get("?participantId=test-participant")
                .then()
                .statusCode(200)
                .log().ifError()
                .extract().body().as(KeyPairResource[].class);
        assertThat(found).isEmpty();

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals("test-participant");
        }));
    }

    @Test
    void findForParticipant_notfound() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.notFound("test-message"));

        baseRequest()
                .get("?participantId=test-participant")
                .then()
                .statusCode(404)
                .log().ifError();

        verify(keyPairService).query(argThat(q -> {
            var criterion = q.getFilterExpression().get(0);
            return criterion.getOperandLeft().equals("participantId") &&
                    criterion.getOperator().equals("=") &&
                    criterion.getOperandRight().equals("test-participant");
        }));
    }

    @ParameterizedTest(name = "Make default: {0}")
    @ValueSource(booleans = {true, false})
    void addKeyPair(boolean makeDefault) {
        var descriptor = createKeyDescriptor()
                .build();
        when(keyPairService.addKeyPair(eq("test-participant"), any(), eq(makeDefault))).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body(descriptor)
                .put("?participantId=%s&makeDefault=%s".formatted("test-participant", makeDefault))
                .then()
                .log().ifError()
                .statusCode(204);

        verify(keyPairService).addKeyPair(eq("test-participant"), argThat(d -> d.getKeyId().equals(descriptor.getKeyId())), eq(makeDefault));
        verifyNoMoreInteractions(keyPairService);
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

    @Override
    protected Object controller() {
        return new KeyPairResourceApiController(authService, keyPairService);
    }

    private KeyPairResource.Builder createKeyPair() {
        return KeyPairResource.Builder.newInstance()
                .id("test-keypair")
                .participantId("test-participant")
                .isDefaultPair(true)
                .privateKeyAlias("test-alias")
                .useDuration(Duration.ofDays(365).toMillis());
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/keypairs")
                .when();
    }

    @NotNull
    private static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .keyId("new-key-id")
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"));
    }
}