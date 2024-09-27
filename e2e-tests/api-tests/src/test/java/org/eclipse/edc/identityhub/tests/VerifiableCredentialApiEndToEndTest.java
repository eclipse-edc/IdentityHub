/*
 *  Copyright (c) 2024 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.Header;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class VerifiableCredentialApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore stsAccountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            stsAccountStore.findAll(QuerySpec.max())
                    .forEach(sts -> stsAccountStore.deleteById(sts.getId()).getContent());

        }

        @Test
        void findById(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            var credential = context.createCredential();
            var resourceId = context.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void create(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var vc = context.createCredential();
                        var resourceId = UUID.randomUUID().toString();
                        var manifest = createManifest(user, vc).id(resourceId).build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .post("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId).orElseThrow(() -> new EdcException("Failed to credential with id %s".formatted(resourceId)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(vc);
                    });
        }

        @Test
        void update(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential1 = context.createCredential();
                        var credential2 = context.createCredential();
                        var resourceId1 = context.storeCredential(credential1, user);
                        var manifest = createManifest(user, credential2).id(resourceId1).build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .put("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId1).orElseThrow(() -> new EdcException("Failed to retrieve credential with id %s".formatted(resourceId1)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(credential2);
                    });
        }

        @Test
        void delete(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential = context.createCredential();
                        var resourceId = context.storeCredential(credential, user);
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .delete("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId);
                        assertThat(resource.isEmpty()).isTrue();
                    });
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

        private VerifiableCredentialManifest.Builder createManifest(String participantId, VerifiableCredential vc) {
            return VerifiableCredentialManifest.Builder.newInstance()
                    .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, vc))
                    .participantId(participantId);
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(IdentityHubEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IdentityHubEndToEndExtension.Postgres.class)
    class Postgres extends Tests {
    }
}
