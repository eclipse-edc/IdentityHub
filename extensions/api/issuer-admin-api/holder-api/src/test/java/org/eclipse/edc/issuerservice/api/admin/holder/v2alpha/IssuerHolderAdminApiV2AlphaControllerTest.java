/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.api.admin.holder.v2alpha;

import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.IssuerHolderAdminApiControllerTest;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
class IssuerHolderAdminApiV2AlphaControllerTest extends IssuerHolderAdminApiControllerTest {

    @Test
    void patchHolder() {
        var patched = createHolder("test-id", "did:web:test", "test name");
        when(holderService.patchHolder(any(), any(), any(), any(), any(), any())).thenReturn(ServiceResult.success(patched));

        var response = baseRequest()
                .body(Map.of("did", "did:web:test", "name", "test name", "properties", Map.of("foo", "bar"), "removed", List.of("baz")))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(Holder.class);

        assertThat(response).usingRecursiveComparison().isEqualTo(patched);
        verify(holderService).patchHolder(any(), eq("test-id"), eq("did:web:test"), eq("test name"), eq(Map.of("foo", "bar")), eq(Set.of("baz")));
    }

    @Test
    void patchHolder_whenRemovedMissing() {
        var patched = createHolder("test-id", "did:web:test", "test name");
        when(holderService.patchHolder(any(), any(), any(), any(), any(), any())).thenReturn(ServiceResult.success(patched));

        baseRequest()
                .body(Map.of("did", "did:web:test", "name", "test name", "properties", Map.of("foo", "bar")))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        verify(holderService).patchHolder(any(), eq("test-id"), eq("did:web:test"), eq("test name"), eq(Map.of("foo", "bar")), eq(Set.of()));
    }

    @Test
    void patchHolder_notFound() {
        when(holderService.patchHolder(any(), any(), any(), any(), any(), any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .body(Map.of("did", "did:web:test", "name", "test name"))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(404)
                .body(notNullValue());
    }

    @Test
    void patchHolder_whenDidOrNameDoNotMatch() {
        when(holderService.patchHolder(any(), any(), any(), any(), any(), any())).thenReturn(ServiceResult.badRequest("does not match"));

        baseRequest()
                .body(Map.of("did", "did:web:other", "name", "test name"))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(notNullValue());
    }

    @Test
    void patchHolder_whenPropertySetAndRemoved() {
        baseRequest()
                .body(Map.of("did", "did:web:test", "properties", Map.of("foo", "bar"), "removed", List.of("foo")))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(400);

        verifyNoInteractions(holderService);
    }

    @Test
    void patchHolder_whenDidMissing() {
        baseRequest()
                .body(Map.of("properties", Map.of("foo", "bar")))
                .patch("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(400);

        verifyNoInteractions(holderService);
    }

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }

    @Override
    protected Object controller() {
        return new IssuerHolderAdminApiV2AlphaController(authorizationService, holderService);
    }
}
