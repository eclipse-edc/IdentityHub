/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did.store.test;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

/**
 * Base test class for DidResourceStore implementations.
 */
public abstract class DidResourceStoreTestBase {

    public static final String DID = "did:web:test";

    @Test
    void save() {
        var didResource = createDidResource(DID).build();
        assertThat(getStore().save(didResource)).isSucceeded();
    }

    @Test
    void save_alreadyExists() {
        var didResource = createDidResource(DID).build();
        assertThat(getStore().save(didResource)).isSucceeded();
        assertThat(getStore().save(didResource)).isFailed().detail().isEqualTo("A DidResource with ID %s already exists.".formatted(DID));
    }

    @Test
    void update() {
        var builder = createDidResource(DID);
        var didResource = builder.build();
        getStore().save(didResource);

        var didResource2 = builder.state(DidState.GENERATED).build();
        assertThat(getStore().update(didResource2)).isSucceeded();
        var fromDb = getStore().findById(DID);
        Assertions.assertThat(fromDb).usingRecursiveComparison().isEqualTo(didResource2);
    }

    @Test
    void update_notExists() {
        assertThat(getStore().update(createDidResource(DID).build())).isFailed()
                .detail().isEqualTo("A DidResource with ID %s was not found.".formatted(DID));
    }

    @Test
    void findById() {
        var didResource = createDidResource(DID).build();
        getStore().save(didResource);

        Assertions.assertThat(getStore().findById(DID))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(didResource);
    }

    @Test
    void findById_notExists() {
        Assertions.assertThat(getStore().findById("did:web:notexist")).isNull();
    }

    @Test
    void query() {
        var dids = range(0, 10)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList();
        dids.forEach(getStore()::save);

        Assertions.assertThat(getStore().query(QuerySpec.none()))
                .hasSize(10)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(dids);
    }

    @Test
    void query_withPage() {
        var dids = range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList();
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().range(new Range(25, 35)).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(10)
                .usingRecursiveFieldByFieldElementComparator()
                .containsAnyElementsOf(dids);
    }

    @Test
    void query_withSorting() {
        var dids = range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList();
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().sortOrder(SortOrder.DESC).sortField("did").build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(50)
                .usingRecursiveFieldByFieldElementComparator()
                .containsAnyElementsOf(dids)
                .extracting(DidResource::getDid)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void query_bySimpleProperty() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69").state(DidState.PUBLISHED).build();
        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().filter(new Criterion("state", "=", DidState.PUBLISHED.code())).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void query_byParticipantId() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69").participantContextId("the-odd-one-out").build();
        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = queryByParticipantContextId(expected.getParticipantContextId()).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void query_byComplexProperty_service() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69")
                .document(DidDocument.Builder.newInstance()
                        .id(DID + "69")
                        .service(List.of(new Service("test-service", "foo-type", "https://foo.bar")))
                        .build())
                .build();

        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().filter(new Criterion("document.service.type", "=", "foo-type")).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void query_byComplexProperty_verificationMethod() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69")
                .document(DidDocument.Builder.newInstance()
                        .id(DID + "69")
                        .service(List.of(new Service("test-service", "foo-type", "https://foo.bar")))
                        .verificationMethod(List.of(VerificationMethod.Builder.newInstance().id("vm-1").type("test-type").publicKeyMultibase("asdfl;aksdflaskdfj").build()))
                        .build())
                .build();

        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().filter(new Criterion("document.verificationMethod.type", "=", "test-type")).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void query_byComplexProperty_authentication() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69")
                .document(DidDocument.Builder.newInstance()
                        .id(DID + "69")
                        .service(List.of(new Service("test-service", "foo-type", "https://foo.bar")))
                        .verificationMethod(List.of(VerificationMethod.Builder.newInstance().id("vm-1").type("test-type").publicKeyMultibase("asdfl;aksdflaskdfj").build()))
                        .authentication(List.of("auth1", "auth2"))
                        .build())
                .build();

        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().filter(new Criterion("document.authentication", "=", "auth1")).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void query_byComplexProperty_id() {
        var dids = new ArrayList<>(range(0, 50)
                .mapToObj(i -> createDidResource(DID + i).build())
                .toList());

        var expected = createDidResource(DID + "69")
                .document(DidDocument.Builder.newInstance()
                        .id(DID + "69")
                        .build())
                .build();

        dids.add(expected);
        dids.forEach(getStore()::save);

        var q = QuerySpec.Builder.newInstance().filter(new Criterion("document.id", "=", DID + "69")).build();
        Assertions.assertThat(getStore().query(q))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    void deleteById() {
        var didResource = createDidResource(DID).build();
        getStore().save(didResource);
        assertThat(getStore().deleteById(DID)).isSucceeded();
        Assertions.assertThat(getStore().query(QuerySpec.none())).isEmpty();
    }

    @Test
    void deleteById_notExist() {
        assertThat(getStore().deleteById(DID)).isFailed()
                .detail().isEqualTo("A DidResource with ID %s was not found.".formatted(DID));
    }

    protected abstract DidResourceStore getStore();

    private DidResource.Builder createDidResource(String did) {
        return DidResource.Builder.newInstance()
                .did(did)
                .participantContextId("test-participant")
                .document(DidDocument.Builder.newInstance()
                        .id(did)
                        .build())
                .state(DidState.INITIAL);
    }
}
