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

package org.eclipse.edc.identityhub.store.test;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState.ACTIVATED;
import static org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState.CREATED;
import static org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState.DEACTIVATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class ParticipantContextStoreTestBase {

    @Test
    void create() {
        var result = getStore().create(createParticipantContext());
        assertThat(result).isSucceeded();

    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var context = createParticipantContext();
        var result = getStore().create(context);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(context);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createParticipantContextBuilder().participantId("id" + i).build())
                .forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantId", "=", "id2"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1));
    }

    @Test
    void query_byProperty() {
        var participantContext = createParticipantContextBuilder().state(DEACTIVATED).build();
        getStore().create(participantContext);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "=", 2))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(participantContext));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContextBuilder().participantId("id" + i).build())
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new ParticipantContext[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContextBuilder()
                        .participantId("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantId", "=", "id7"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContextBuilder()
                        .participantId("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var context = createParticipantContextBuilder();
        var result = getStore().create(context.build());
        assertThat(result).isSucceeded();

        var updateRes = getStore().update(context.state(ACTIVATED).build());
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var context = createParticipantContextBuilder();
        var result = getStore().create(context.build());

        var updateRes = getStore().update(context.state(DEACTIVATED).participantId("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID another-id was not found");
    }

    @Test
    void update_whenNotExists() {
        var context = createParticipantContextBuilder();
        var updateRes = getStore().update(context.state(DEACTIVATED).participantId("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID another-id was not found");
    }

    @Test
    void delete() {
        var context = createParticipantContext();
        getStore().create(context);

        var deleteRes = getStore().deleteById(context.getParticipantId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID not-exist was not found");
    }

    protected abstract ParticipantContextStore getStore();

    private ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId("test-participant")
                .state(CREATED)
                .apiTokenAlias("test-alias")
                .build();
    }

    private ParticipantContext.Builder createParticipantContextBuilder() {
        return ParticipantContext.Builder.newInstance()
                .participantId("test-participant")
                .state(CREATED)
                .apiTokenAlias("test-alias");
    }
}
