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

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.participantcontext.ApiTokenGenerator;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@EndToEndTest
public class ParticipantContextApiEndToEndTest {

    public static final String SUPER_USER = "super-user";
    public static final String SUPER_USER_ALIAS = "super-alias";
    protected static final IdentityHubRuntimeConfiguration RUNTIME_CONFIGURATION = IdentityHubRuntimeConfiguration.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    @RegisterExtension
    private static final EdcRuntimeExtension RUNTIME = new EdcRuntimeExtension(":launcher", "identity-hub", RUNTIME_CONFIGURATION.controlPlaneConfiguration());

    @Test
    void getUserById() {
        var pc = ParticipantContext.Builder.newInstance()
                .participantId(SUPER_USER)
                .did("did:web:superuser")
                .apiTokenAlias(SUPER_USER_ALIAS)
                .build();
        storeParticipant(pc);

        var su = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", createTokenFor(SUPER_USER)))
                .get("/v1/participants/" + SUPER_USER)
                .then()
                .statusCode(200)
                .extract().body().as(ParticipantContext.class);
        assertThat(su).usingRecursiveComparison().isEqualTo(pc);
    }

    @Test
    void createNewUser_principalIsAdmin() {
        var pc = ParticipantContext.Builder.newInstance()
                .participantId(SUPER_USER)
                .did("did:web:superuser")
                .apiTokenAlias(SUPER_USER_ALIAS)
                .roles(List.of("admin"))
                .build();
        var apiToken = storeParticipant(pc);

        var manifest = createNewParticipant();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apiToken))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1/participants/")
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)))
                .body(notNullValue());
    }

    @Test
    void createNewUser_principalIsNotAdmin_expect403() {
        var pc = ParticipantContext.Builder.newInstance()
                .participantId(SUPER_USER)
                .did("did:web:superuser")
                .apiTokenAlias(SUPER_USER_ALIAS)
                .roles(List.of(/*admin role not assigned*/))
                .build();
        var apiToken = storeParticipant(pc);

        var manifest = createNewParticipant();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apiToken))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1/participants/")
                .then()
                .log().ifError()
                .statusCode(403)
                .body(notNullValue());
    }

    private String createTokenFor(String userId) {
        return new ApiTokenGenerator().generate(userId);
    }

    private String storeParticipant(ParticipantContext pc) {
        var store = RUNTIME.getContext().getService(ParticipantContextStore.class);

        var vault = RUNTIME.getContext().getService(Vault.class);
        var token = createTokenFor(pc.getParticipantId());
        vault.storeSecret(pc.getApiTokenAlias(), token);
        store.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    private static ParticipantManifest createNewParticipant() {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant")
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias("another-alias")
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId("another-keyid")
                        .build())
                .build();
        return manifest;
    }
}
