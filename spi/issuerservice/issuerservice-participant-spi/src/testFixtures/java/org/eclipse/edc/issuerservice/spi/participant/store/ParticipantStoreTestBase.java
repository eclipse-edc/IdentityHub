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

package org.eclipse.edc.issuerservice.spi.participant.store;

import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class ParticipantStoreTestBase {

    @Test
    void create() {
        var participantContext = createParticipant();
        var result = getStore().create(participantContext);
        assertThat(result).isSucceeded();
        var query = getStore().query(QuerySpec.max());
        assertThat(query).isSucceeded();
        assertThat(query.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(participantContext);
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var context = createParticipant();
        var result = getStore().create(context);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(context);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> new Participant("p" + i, "did:web:" + i, "participant" + i))
                .forEach(getStore()::create);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantId", "=", "p4"))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> assertThat(str).hasSize(1));
    }

    @Test
    void query_byDid() {

        var participantContext = createParticipant();
        getStore().create(participantContext);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("did", "=", "did:web:participant"))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> assertThat(str)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(participantContext));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> new Participant("p" + i, "did:web:" + i, "participant" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new Participant[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> new Participant("p" + i, "did:web:" + i, "participant" + i))
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", "p99"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> new Participant("p" + i, "did:web:" + i, "participant" + i))
                .toList();


        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var participant = createParticipant();
        var result = getStore().create(participant);
        assertThat(result).isSucceeded();

        var updated = new Participant("p-id", "did:web:participant-changed", "participant-changed");
        var updateRes = getStore().update(updated);
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var participant = createParticipant();
        getStore().create(participant);

        var updated = new Participant("p16", "did:web:participant", "participant14");
        assertThat(getStore().update(updated)).isFailed().detail().contains("with ID 'p16' does not exist.");
    }

    @Test
    void update_whenNotExists() {
        var participant = createParticipant();

        var updateRes = getStore().update(participant);
        assertThat(updateRes).isFailed().detail().contains("with ID 'p-id' does not exist.");
    }

    @Test
    void delete() {
        var context = createParticipant();
        getStore().create(context);

        var deleteRes = getStore().deleteById(context.participantId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' does not exist.");
    }

    protected abstract ParticipantStore getStore();

    private Participant createParticipant() {
        return new Participant("p-id", "did:web:participant", "participant display name", List.of("att1", "att2"));
    }

}
