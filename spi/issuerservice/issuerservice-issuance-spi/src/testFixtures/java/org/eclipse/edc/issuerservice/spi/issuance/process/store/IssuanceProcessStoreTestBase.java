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

package org.eclipse.edc.issuerservice.spi.issuance.process.store;

import org.awaitility.Awaitility;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
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
import java.util.Map;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.APPROVED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.DELIVERED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.hasSize;

public abstract class IssuanceProcessStoreTestBase {

    protected static final String RUNTIME_ID = "runtime-id";
    protected final Clock clock = Clock.systemUTC();


    /**
     * determines the amount of time (default = 500ms) before an async test using Awaitility fails. This may be useful if using remote
     * or non-self-contained databases.
     */
    protected Duration getTestTimeout() {
        return Duration.ofMillis(500);
    }

    protected abstract IssuanceProcessStore getStore();

    protected abstract void leaseEntity(String issuanceId, String owner, Duration duration);

    protected void leaseEntity(String negotiationId, String owner) {
        leaseEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLeasedBy(String issuanceId, String owner);

    private IssuanceProcess createIssuanceProcess() {
        return createIssuanceProcess(UUID.randomUUID().toString());
    }

    private IssuanceProcess createIssuanceProcess(String id, IssuanceProcessStates state) {
        return createIssuanceProcessBuilder().id(id).state(state.code()).build();
    }

    private IssuanceProcess createIssuanceProcess(String id) {
        return createIssuanceProcess(id, APPROVED);
    }

    private IssuanceProcess.Builder createIssuanceProcessBuilder() {
        return IssuanceProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(UUID.randomUUID().toString())
                .holderId(UUID.randomUUID().toString())
                .credentialFormats(Map.of("format", CredentialFormat.VC1_0_JWT))
                .holderPid(UUID.randomUUID().toString())
                .state(APPROVED.code());
    }

    @Nested
    class Create {

        @Test
        void shouldCreate() {
            var issuanceProcess = createIssuanceProcess();
            getStore().save(issuanceProcess);
            var retrieved = getStore().findById(issuanceProcess.getId());

            assertThat(retrieved).isNotNull().usingRecursiveComparison().isEqualTo(issuanceProcess);
            assertThat(retrieved.getCreatedAt()).isNotEqualTo(0L);
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldFindEntityById() {
            var issuanceProcess = createIssuanceProcess();
            getStore().save(issuanceProcess);

            var result = getStore().findById(issuanceProcess.getId());

            assertThat(result).usingRecursiveComparison().isEqualTo(issuanceProcess);
        }

        @Test
        void notExist() {
            var result = getStore().findById("not-exist");

            assertThat(result).isNull();
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = APPROVED;
            var all = range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i, state))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().nextNotLeased(5, hasState(state.code())))
                    .hasSize(5)
                    .extracting(IssuanceProcess::getId)
                    .isSubsetOf(all.stream().map(IssuanceProcess::getId).toList())
                    .allMatch(id -> isLeasedBy(id, RUNTIME_ID));
        }

        @Test
        void shouldOnlyReturnFreeItems() {
            var state = APPROVED;
            var all = range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i, state))
                    .peek(getStore()::save)
                    .toList();

            // lease a few
            var leasedProcesses = all.stream().skip(5).peek(ip -> leaseEntity(ip.getId(), RUNTIME_ID)).toList();

            // should not contain leased IPs
            assertThat(getStore().nextNotLeased(10, hasState(state.code())))
                    .hasSize(5)
                    .isSubsetOf(all)
                    .doesNotContainAnyElementsOf(leasedProcesses);
        }

        @Test
        void noFreeItem_shouldReturnEmpty() {
            var state = APPROVED;
            range(0, 3)
                    .mapToObj(i -> createIssuanceProcess("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).hasSize(3);
            // second time returns empty list
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).isEmpty();
        }

        @Test
        void noneInDesiredState() {
            range(0, 3)
                    .mapToObj(i -> createIssuanceProcess("id" + i, APPROVED))
                    .forEach(getStore()::save);

            var nextNotLeased = getStore().nextNotLeased(10, hasState(DELIVERED.code()));

            assertThat(nextNotLeased).isEmpty();
        }

        @Test
        void batchSizeLimits() {
            var state = APPROVED;
            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            var result = getStore().nextNotLeased(3, hasState(state.code()));
            assertThat(result).hasSize(3);
        }

        @Test
        void verifyTemporalOrdering() {
            var state = APPROVED;
            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess(String.valueOf(i), state))
                    .peek(this::delayByTenMillis)
                    .forEach(getStore()::save);

            assertThat(getStore().nextNotLeased(20, hasState(state.code())))
                    .extracting(IssuanceProcess::getId)
                    .map(Integer::parseInt)
                    .isSortedAccordingTo(Integer::compareTo);
        }

        @Test
        void verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
            var all = range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i, APPROVED))
                    .peek(getStore()::save)
                    .toList();

            Thread.sleep(100);

            var fourth = all.get(3);
            fourth.updateStateTimestamp();
            getStore().save(fourth);

            var next = getStore().nextNotLeased(20, hasState(APPROVED.code()));
            assertThat(next.indexOf(fourth)).isEqualTo(9);
        }

        @Test
        @DisplayName("Verifies that calling nextNotLeased locks the IP for any subsequent calls")
        void locksEntity() {
            var issuanceProcess = createIssuanceProcess("id1", APPROVED);
            getStore().save(issuanceProcess);

            getStore().nextNotLeased(100, hasState(APPROVED.code()));

            assertThat(isLeasedBy(issuanceProcess.getId(), RUNTIME_ID)).isTrue();
        }

        @Test
        void expiredLease() {
            var issuanceProcess = createIssuanceProcess("id1", APPROVED);
            getStore().save(issuanceProcess);

            leaseEntity(issuanceProcess.getId(), RUNTIME_ID, Duration.ofMillis(100));

            Awaitility.await().atLeast(Duration.ofMillis(100))
                    .atMost(getTestTimeout())
                    .until(() -> getStore().nextNotLeased(10, hasState(APPROVED.code())), hasSize(1));
        }

        @Test
        void shouldLeaseEntityUntilUpdate() {
            var issuanceProcess = createIssuanceProcess();
            getStore().save(issuanceProcess);

            var firstQueryResult = getStore().nextNotLeased(1, hasState(APPROVED.code()));
            assertThat(firstQueryResult).hasSize(1);

            var secondQueryResult = getStore().nextNotLeased(1, hasState(APPROVED.code()));
            assertThat(secondQueryResult).hasSize(0);

            var retrieved = firstQueryResult.get(0);
            getStore().save(retrieved);

            var thirdQueryResult = getStore().nextNotLeased(1, hasState(APPROVED.code()));
            assertThat(thirdQueryResult).hasSize(1);
        }

        @Test
        void avoidsStarvation() throws InterruptedException {
            for (var i = 0; i < 10; i++) {
                var process = createIssuanceProcess("test-process-" + i);
                getStore().save(process);
            }

            var list1 = getStore().nextNotLeased(5, hasState(APPROVED.code()));
            Thread.sleep(50); //simulate a short delay to generate different timestamps
            list1.forEach(ip -> {
                ip.updateStateTimestamp();
                getStore().save(ip);
            });
            var list2 = getStore().nextNotLeased(5, hasState(APPROVED.code()));
            assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
        }

        @Test
        void shouldLeaseOrderByStateTimestamp() {

            var all = range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id-" + i))
                    .peek(getStore()::save)
                    .toList();

            all.stream().limit(5)
                    .peek(this::delayByTenMillis)
                    .sorted(Comparator.comparing(IssuanceProcess::getStateTimestamp).reversed())
                    .forEach(f -> getStore().save(f));

            var elements = getStore().nextNotLeased(10, hasState(APPROVED.code()));
            assertThat(elements).hasSize(10).extracting(IssuanceProcess::getStateTimestamp).isSorted();
        }

        private void delayByTenMillis(IssuanceProcess t) {
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
            var issuanceProcess = createIssuanceProcess();
            getStore().save(issuanceProcess);

            issuanceProcess.transitionToDelivered();

            getStore().save(issuanceProcess);

            assertThat(getStore().query(QuerySpec.none()))
                    .hasSize(1)
                    .first().satisfies(actual -> {
                        assertThat(actual.getState()).isEqualTo(DELIVERED.code());
                    });
        }

        @Test
        @DisplayName("Verify that the lease on a IP is cleared by an update")
        void shouldBreakLease() {
            var issuanceProcess = createIssuanceProcess("id1");
            getStore().save(issuanceProcess);
            // acquire lease
            leaseEntity(issuanceProcess.getId(), RUNTIME_ID);

            issuanceProcess = issuanceProcess.toBuilder().state(DELIVERED.code()).build();
            getStore().save(issuanceProcess);

            // lease should be broken
            var notLeased = getStore().nextNotLeased(10, hasState(DELIVERED.code()));

            assertThat(notLeased).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void leasedByOther_shouldThrowException() {
            var id = "id1";
            var issuanceProcess = createIssuanceProcess(id);
            getStore().save(issuanceProcess);
            leaseEntity(id, "someone");

            var updatedIssuanceProcess = issuanceProcess.toBuilder().state(DELIVERED.code()).build();

            // leased by someone else -> throw exception
            assertThatThrownBy(() -> getStore().save(updatedIssuanceProcess)).isInstanceOf(IllegalStateException.class);
        }

    }

    @Nested
    class FindAll {

        @Test
        void noQuerySpec() {
            var all = range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().query(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
        }

        @Test
        void verifyFiltering() {
            range(0, 10).forEach(i -> getStore().save(createIssuanceProcess("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "test-neg-3")).build();

            var result = getStore().query(querySpec);

            assertThat(result).extracting(IssuanceProcess::getId).containsOnly("test-neg-3");
        }

        @Test
        void shouldThrowException_whenInvalidOperator() {
            range(0, 10).forEach(i -> getStore().save(createIssuanceProcess("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "foobar", "other")).build();

            assertThatThrownBy(() -> getStore().query(querySpec).toList()).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void queryByState() {
            var issuanceProcess = createIssuanceProcess("testprocess1");
            getStore().save(issuanceProcess);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", issuanceProcess.getState())))
                    .build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void queryByStateAsString() {
            var issuanceProcess = createIssuanceProcess("testprocess1");
            getStore().save(issuanceProcess);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", IssuanceProcessStates.from(issuanceProcess.getState()).name())))
                    .build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void queryByHolderId() {

            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i))
                    .forEach(getStore()::save);

            var issuanceProcess = createIssuanceProcessBuilder().id("testprocess1").holderId("participant1").build();

            getStore().save(issuanceProcess);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("holderId", "=", issuanceProcess.getHolderId())))
                    .build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void queryByParticipantContextId() {

            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i))
                    .forEach(getStore()::save);

            var issuanceProcess = createIssuanceProcessBuilder().id("testprocess1").holderId("participant1").build();

            getStore().save(issuanceProcess);

            var query = ParticipantResource.queryByParticipantContextId(issuanceProcess.getParticipantContextId()).build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void queryByCredentialDefinition() {

            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i))
                    .forEach(getStore()::save);

            var issuanceProcess = createIssuanceProcessBuilder().id("testprocess1")
                    .credentialDefinitions("cred-def")
                    .build();
            getStore().save(issuanceProcess);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("credentialDefinitions", "contains", "cred-def")))
                    .build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void queryByClaims() {
            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess("id" + i))
                    .forEach(getStore()::save);

            var issuanceProcess = createIssuanceProcessBuilder().id("testprocess1")
                    .claims(Map.of("subclaims", Map.of("name", "value")))
                    .build();
            getStore().save(issuanceProcess);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("claims.subclaims.name", "=", "value")))
                    .build();

            var result = getStore().query(query).toList();
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(issuanceProcess);
        }

        @Test
        void verifySorting() {
            range(0, 10).forEach(i -> getStore().save(createIssuanceProcess("test-neg-" + i)));

            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()))
                    .hasSize(10)
                    .isSortedAccordingTo(Comparator.comparing(IssuanceProcess::getId));
            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()))
                    .hasSize(10)
                    .isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
        }

        @Test
        void verifyPaging() {
            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
            assertThat(getStore().query(qs)).hasSize(5)
                    .extracting(IssuanceProcess::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 8);
        }

        @Test
        void verifyPaging_pageSizeLargerThanCollection() {

            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
            assertThat(getStore().query(qs))
                    .hasSize(7)
                    .extracting(IssuanceProcess::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 10);
        }

        @Test
        void verifyPaging_pageSizeOutsideCollection() {

            range(0, 10)
                    .mapToObj(i -> createIssuanceProcess(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
            assertThat(getStore().query(qs)).isEmpty();

        }
    }
}
