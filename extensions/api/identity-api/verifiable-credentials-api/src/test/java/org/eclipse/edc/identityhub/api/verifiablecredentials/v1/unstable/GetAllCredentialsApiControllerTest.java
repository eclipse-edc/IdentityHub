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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable;

import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class GetAllCredentialsApiControllerTest extends RestControllerTestBase {

    private final CredentialStore credentialStore = mock();

    @Test
    void getAll_sortsById() {
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.<VerifiableCredentialResource>of()));

        given()
                .when()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.STABLE + "/credentials")
                .get("?offset=10&limit=20")
                .then()
                .statusCode(200);

        verify(credentialStore).query(argThat(q -> "id".equals(q.getSortField()) && q.getSortOrder() == SortOrder.ASC &&
                q.getOffset() == 10 && q.getLimit() == 20));
    }

    @Override
    protected Object controller() {
        return new GetAllCredentialsApiController(credentialStore);
    }
}
