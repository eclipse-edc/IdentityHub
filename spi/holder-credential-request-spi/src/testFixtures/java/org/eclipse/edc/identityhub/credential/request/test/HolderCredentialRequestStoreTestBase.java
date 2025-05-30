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

package org.eclipse.edc.identityhub.credential.request.test;

import org.awaitility.Awaitility;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.hasSize;

/**
 * Base test class for {@link org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore}
 * implementations
 */
public abstract class HolderCredentialRequestStoreTestBase {
    protected static final String RUNTIME_ID = "runtime-Id";
    protected final Clock clock = Clock.systemUTC();

    protected abstract HolderCredentialRequestStore getStore();

    protected abstract boolean isLeasedBy(String issuanceId, String owner);

    protected void leaseEntity(String requestId, String owner) {
        leaseEntity(requestId, owner, Duration.ofSeconds(60));
    }

    protected abstract void leaseEntity(String issuanceId, String owner, Duration duration);

    /**
     * determines the amount of time (default = 500ms) before an async test using Awaitility fails. This may be useful if using remote
     * or non-self-contained databases.
     */
    protected Duration getTestTimeout() {
        return Duration.ofMillis(500);
    }

    private HolderCredentialRequest createHolderRequest(String id, HolderRequestState state) {
        return createHolderRequestBuilder()
                .id(id)
                .state(state.code())
                .build();
    }

    private HolderCredentialRequest createHolderRequest() {
        return createHolderRequest(UUID.randomUUID().toString(), CREATED);
    }

    private HolderCredentialRequest createHolderRequest(String id) {
        return createHolderRequest(id, CREATED);
    }

    private HolderCredentialRequest.Builder createHolderRequestBuilder() {
        return HolderCredentialRequest.Builder.newInstance()
                .requestedCredential("test-credential-id", "TestCredential", "VC1_0_JWT")
                .state(CREATED.code())
                .id("test-id")
                .participantContextId("test-participant")
                .issuerDid("did:web:testissuer");
    }

    @Nested
    class Save {

        @Test
        void save() {

        }

        @Test
        void save_whenAlreadyExists_expectUpdate() {

        }
    }

    @Nested
    class FindById {
        @Test
        void findById() {
        }

        @Test
        void findById_whenNotFound_expectNotFound() {
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = ISSUED;
            var all = range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i, state))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().nextNotLeased(5, hasState(state.code())))
                    .hasSize(5)
                    .extracting(HolderCredentialRequest::getId)
                    .isSubsetOf(all.stream().map(HolderCredentialRequest::getId).toList())
                    .allMatch(id -> isLeasedBy(id, RUNTIME_ID));
        }

        @Test
        void shouldOnlyReturnFreeItems() {
            var state = ISSUED;
            var all = range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i, state))
                    .peek(getStore()::save)
                    .toList();

            // lease a few
            var leasedProcesses = all.stream().skip(5).peek(rq -> leaseEntity(rq.getId(), RUNTIME_ID)).toList();

            // should not contain leased IPs
            assertThat(getStore().nextNotLeased(10, hasState(state.code())))
                    .hasSize(5)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isSubsetOf(all)
                    .doesNotContainAnyElementsOf(leasedProcesses);
        }

        @Test
        void noFreeItem_shouldReturnEmpty() {
            var state = ISSUED;
            range(0, 3)
                    .mapToObj(i -> createHolderRequest("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).hasSize(3);
            // second time returns empty list
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).isEmpty();
        }

        @Test
        void noneInDesiredState() {
            range(0, 3)
                    .mapToObj(i -> createHolderRequest("id" + i, ISSUED))
                    .forEach(getStore()::save);

            var nextNotLeased = getStore().nextNotLeased(10, hasState(HolderRequestState.REQUESTING.code()));

            assertThat(nextNotLeased).isEmpty();
        }

        @Test
        void batchSizeLimits() {
            var state = ISSUED;
            range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            var result = getStore().nextNotLeased(3, hasState(state.code()));
            assertThat(result).hasSize(3);
        }

        @Test
        void verifyTemporalOrdering() {
            var state = ISSUED;
            range(0, 10)
                    .mapToObj(i -> createHolderRequest(String.valueOf(i), state))
                    .peek(this::delayByTenMillis)
                    .forEach(getStore()::save);

            assertThat(getStore().nextNotLeased(20, hasState(state.code())))
                    .extracting(HolderCredentialRequest::getId)
                    .map(Integer::parseInt)
                    .isSortedAccordingTo(Integer::compareTo);
        }

        @Test
        void verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
            var all = range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i, ISSUED))
                    .peek(getStore()::save)
                    .toList();

            Thread.sleep(100);

            var fourth = all.get(3);
            fourth.updateStateTimestamp();
            getStore().save(fourth);

            var next = getStore().nextNotLeased(20, hasState(ISSUED.code()));
            assertThat(next.indexOf(fourth)).isEqualTo(9);
        }

        @Test
        @DisplayName("Verifies that calling nextNotLeased locks the IP for any subsequent calls")
        void locksEntity() {
            var request = createHolderRequest("id1", ISSUED);
            getStore().save(request);

            getStore().nextNotLeased(100, hasState(ISSUED.code()));

            assertThat(isLeasedBy(request.getId(), RUNTIME_ID)).isTrue();
        }

        @Test
        void expiredLease() {
            var request = createHolderRequest("id1", ISSUED);
            getStore().save(request);

            leaseEntity(request.getId(), RUNTIME_ID, Duration.ofMillis(100));

            Awaitility.await().atLeast(Duration.ofMillis(100))
                    .atMost(getTestTimeout())
                    .until(() -> getStore().nextNotLeased(10, hasState(ISSUED.code())), hasSize(1));
        }

        @Test
        void shouldLeaseEntityUntilUpdate() {
            var request = createHolderRequestBuilder().state(ISSUED.code()).build();
            getStore().save(request);

            var firstQueryResult = getStore().nextNotLeased(1, hasState(ISSUED.code()));
            assertThat(firstQueryResult).hasSize(1);

            var secondQueryResult = getStore().nextNotLeased(1, hasState(ISSUED.code()));
            assertThat(secondQueryResult).hasSize(0);

            var retrieved = firstQueryResult.get(0);
            getStore().save(retrieved);

            var thirdQueryResult = getStore().nextNotLeased(1, hasState(ISSUED.code()));
            assertThat(thirdQueryResult).hasSize(1);
        }

        @Test
        void avoidsStarvation() throws InterruptedException {
            var store = getStore();
            for (var i = 0; i < 10; i++) {
                var process = createHolderRequest("test-request-" + i, ISSUED);
                store.save(process);
            }

            var list1 = store.nextNotLeased(5, hasState(ISSUED.code()));
            Thread.sleep(50); //simulate a short delay to generate different timestamps
            list1.forEach(rq -> {
                rq.updateStateTimestamp();
                store.save(rq);
            });
            var list2 = store.nextNotLeased(5, hasState(ISSUED.code()));
            assertThat(list1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isNotEqualTo(list2)
                    .doesNotContainAnyElementsOf(list2);
        }

        @Test
        void shouldLeaseOrderByStateTimestamp() {

            var all = range(0, 10)
                    .mapToObj(i -> createHolderRequest("id-" + i, ISSUED))
                    .peek(getStore()::save)
                    .toList();

            all.stream().limit(5)
                    .peek(this::delayByTenMillis)
                    .sorted(Comparator.comparing(HolderCredentialRequest::getStateTimestamp).reversed())
                    .forEach(f -> getStore().save(f));

            var elements = getStore().nextNotLeased(10, hasState(ISSUED.code()));
            assertThat(elements).hasSize(10).extracting(HolderCredentialRequest::getStateTimestamp).isSorted();
        }

        private void delayByTenMillis(HolderCredentialRequest t) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                // noop
            }
            t.updateStateTimestamp();
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdate() {
            var request = createHolderRequest();
            getStore().save(request);

            request.transitionCreated();

            getStore().save(request);

            assertThat(getStore().query(QuerySpec.none()))
                    .hasSize(1)
                    .first().satisfies(actual -> {
                        assertThat(actual.getState()).isEqualTo(CREATED.code());
                    });
        }

        @Test
        @DisplayName("Verify that the lease on a IP is cleared by an update")
        void shouldBreakLease() {
            var request = createHolderRequest("id1");
            getStore().save(request);
            // acquire lease
            leaseEntity(request.getId(), RUNTIME_ID);

            request.transitionCreated();
            getStore().save(request);

            // lease should be broken
            var notLeased = getStore().nextNotLeased(10, hasState(CREATED.code()));

            assertThat(notLeased).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void leasedByOther_shouldThrowException() {
            var id = "id1";
            var request = createHolderRequest(id);
            getStore().save(request);
            leaseEntity(id, "someone");

            request.transitionCreated();

            // leased by someone else -> throw exception
            assertThatThrownBy(() -> getStore().save(request)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Query {

        @Test
        void noQuerySpec() {
            var all = range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().query(QuerySpec.none()))
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrderElementsOf(all);
        }

        @Test
        void verifyFiltering() {
            range(0, 10).forEach(i -> getStore().save(createHolderRequest("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "test-neg-3")).build();

            var result = getStore().query(querySpec);

            assertThat(result).extracting(HolderCredentialRequest::getId).containsOnly("test-neg-3");
        }

        @Test
        void shouldThrowException_whenInvalidOperator() {
            range(0, 10).forEach(i -> getStore().save(createHolderRequest("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "foobar", "other")).build();

            assertThatThrownBy(() -> getStore().query(querySpec)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void queryByState() {
            var request = createHolderRequest("testprocess1");
            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", request.getState())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByStateAsString() {
            var request = createHolderRequest("testprocess1");
            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", HolderRequestState.from(request.getState()).name())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByIssuerDid() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i))
                    .forEach(getStore()::save);

            var request = createHolderRequestBuilder()
                    .id("testprocess1").issuerDid("did:web:test").build();

            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("issuerDid", "=", request.getIssuerDid())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByParticipantContextId() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequestBuilder().id("id" + i).participantContextId("participantContext").build())
                    .forEach(getStore()::save);

            var request = createHolderRequestBuilder()
                    .id("testprocess1")
                    .participantContextId("another-participant-context").build();

            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("participantContextId", "=", request.getParticipantContextId())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByissuerPid() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i))
                    .forEach(getStore()::save);

            var request = createHolderRequestBuilder()
                    .id("testprocess1")
                    .issuerPid("test-issuance-process").build();

            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("issuerPid", "=", request.getIssuerPid())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByCredentialObjectId() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequest("id" + i))
                    .forEach(getStore()::save);

            var request = createHolderRequestBuilder().id("testprocess1")
                    .requestedCredential("FooBarCredential-id", "FooBarCredential", "VC1_0_JWT")
                    .requestedCredential("BarBazCredential-id", "BarBazCredential", "VC1_0_JWT")
                    .build();
            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("idsAndFormats.id", "=", "FooBarCredential-id")))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void verifySorting() {
            range(0, 10).forEach(i -> getStore().save(createHolderRequest("test-neg-" + i)));

            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).stream())
                    .hasSize(10)
                    .isSortedAccordingTo(Comparator.comparing(HolderCredentialRequest::getId));
            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).stream())
                    .hasSize(10)
                    .isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
        }

        @Test
        void verifyPaging() {
            range(0, 10)
                    .mapToObj(i -> createHolderRequest(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
            assertThat(getStore().query(qs)).hasSize(5)
                    .extracting(HolderCredentialRequest::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 8);
        }

        @Test
        void verifyPaging_pageSizeLargerThanCollection() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequest(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
            assertThat(getStore().query(qs))
                    .hasSize(7)
                    .extracting(HolderCredentialRequest::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 10);
        }

        @Test
        void verifyPaging_pageSizeOutsideCollection() {

            range(0, 10)
                    .mapToObj(i -> createHolderRequest(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
            assertThat(getStore().query(qs)).isEmpty();

        }
    }
}
