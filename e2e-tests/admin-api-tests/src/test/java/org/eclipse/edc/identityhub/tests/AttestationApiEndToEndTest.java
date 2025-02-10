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

package org.eclipse.edc.identityhub.tests;

import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AttestationApiEndToEndTest {
    abstract static class Tests {

        @AfterEach
        void teardown(AttestationDefinitionStore store, ParticipantStore participantStore) {
            store.query(QuerySpec.max()).getContent()
                    .forEach(att -> store.deleteById(att.id()));

            participantStore.query(QuerySpec.max()).getContent()
                    .forEach(participant -> participantStore.deleteById(participant.participantId()));
        }

        @Test
        void createAttestationDefinition(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store) {
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar")))
                    .post("/v1alpha/attestations")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(store.resolveDefinition("test-id")).isNotNull();
        }

        @Test
        void getForParticipant(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var att1 = new AttestationDefinition("att1", "test-type", Map.of("bar", "baz"));
            var att2 = new AttestationDefinition("att2", "test-type", Map.of("bar", "baz"));
            var p = new Participant("foobar", "did:web:foobar", "Foo Bar", List.of("att1", "att2"));
            var r = store.create(att1).compose(v -> store.create(att2)).compose(participant -> participantStore.create(p));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .get("/v1alpha/attestations?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(2))
                    .body("[0].id", equalTo("att1"))
                    .body("[1].id", equalTo("att2"));
        }

        @Test
        void linkAttestation_expect201(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var r = store.create(new AttestationDefinition("att1", "test-type", Map.of("bar", "baz")))
                    .compose(participant -> participantStore.create(new Participant("foobar", "did:web:foobar", "Foo Bar")));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/attestations/att1/link?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(participantStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.attestations()).containsExactly("att1"));
        }

        @Test
        void linkAttestation_alreadyLinked_expect204(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var r = store.create(new AttestationDefinition("att1", "test-type", Map.of("bar", "baz")))
                    .compose(participant -> participantStore.create(new Participant("foobar", "did:web:foobar", "Foo Bar", singletonList("att1"))));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/attestations/att1/link?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(participantStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.attestations()).containsExactly("att1"));
        }

        @Test
        void queryAttestations(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var p1 = new Participant("p1", "did:web:foobar", "Foo Bar", singletonList("att1"));
            var p2 = new Participant("p2", "did:web:barbaz", "Bar Baz", List.of("att1", "att2"));

            var r = participantStore.create(p1).compose(participant -> participantStore.create(p2));
            assertThat(r).isSucceeded();
            store.create(new AttestationDefinition("att1", "test-type", Map.of("key1", "val1")));
            store.create(new AttestationDefinition("att2", "test-type", Map.of("key2", "val2")));

            //query by attestation type
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("attestationType", "=", "test-type"))
                            .build())
                    .post("/v1alpha/attestations/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(2))
                    .body("[0].id", equalTo("att1"))
                    .body("[1].id", equalTo("att2"));

        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IssuerServiceEndToEndExtension.Postgres.class)
    class Postgres extends Tests {

    }
}
