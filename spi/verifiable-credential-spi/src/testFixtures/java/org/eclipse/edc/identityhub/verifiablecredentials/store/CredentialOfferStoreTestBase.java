/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.verifiablecredentials.store;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
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
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.PROCESSED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.RECEIVED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.hasSize;

public abstract class CredentialOfferStoreTestBase {
    protected static final String RUNTIME_ID = "runtime-Id";
    protected final Clock clock = Clock.systemUTC();

    protected abstract CredentialOfferStore getStore();

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

    private CredentialOffer.Builder createOfferBuilder() {
        return CredentialOffer.Builder.newInstance()
                .participantContextId("test-participant")
                .id(UUID.randomUUID().toString())
                .issuer("did:web:issuer")
                .state(RECEIVED.code());
    }

    private CredentialOffer createOffer(String id) {
        return createOffer(id, RECEIVED);
    }

    private CredentialOffer createOffer(String id, CredentialOfferStatus state) {

        return createOfferBuilder()
                .id(id)
                .state(state.code())
                .credentialObject(CredentialObject.Builder.newInstance()
                        .profile("test-profile")
                        .bindingMethod("did:web")
                        .credentialType("TestCredential")
                        .issuancePolicy(PresentationDefinition.Builder.newInstance()
                                .id(UUID.randomUUID().toString())
                                .purpose("test-purpose")
                                .name("test-name")
                                .build())
                        .build())
                .build();
    }

    @Nested
    class Save {
        @Test
        void save_whenNotExists_shouldCreate() {
            var offer = createOffer("test-offer", RECEIVED);
            getStore().save(offer);

            assertThat(getStore().query(QuerySpec.max())).isNotNull()
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(offer);
        }

        @Test
        void save_whenExists_shouldUpdate() {
            var offer = createOffer("test-offer", RECEIVED);
            getStore().save(offer);

            var updatedOffer = createOffer("test-offer", PROCESSED);
            getStore().save(updatedOffer);

            var query = getStore().query(QuerySpec.max());
            assertThat(query).isNotNull()
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(updatedOffer);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById() {
            var offer = createOffer("test-offer", RECEIVED);
            getStore().save(offer);

            assertThat(getStore().findById("test-offer")).usingRecursiveComparison().isEqualTo(offer);
        }

        @Test
        void findById_whenNotFound_expectNotFound() {
            assertThat(getStore().findById("test-offer")).isNull();
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = RECEIVED;
            var all = range(0, 10)
                    .mapToObj(i -> createOffer("id" + i, state))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().nextNotLeased(5, hasState(state.code())))
                    .hasSize(5)
                    .extracting(CredentialOffer::getId)
                    .isSubsetOf(all.stream().map(CredentialOffer::getId).toList())
                    .allMatch(id -> isLeasedBy(id, RUNTIME_ID));
        }

        @Test
        void shouldOnlyReturnFreeItems() {
            var state = RECEIVED;
            var all = range(0, 10)
                    .mapToObj(i -> createOffer("id" + i, state))
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
            var state = RECEIVED;
            range(0, 3)
                    .mapToObj(i -> createOffer("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).hasSize(3);
            // second time returns empty list
            assertThat(getStore().nextNotLeased(10, hasState(state.code()))).isEmpty();
        }

        @Test
        void noneInDesiredState() {
            range(0, 3)
                    .mapToObj(i -> createOffer("id" + i, RECEIVED))
                    .forEach(getStore()::save);

            var nextNotLeased = getStore().nextNotLeased(10, hasState(HolderRequestState.REQUESTING.code()));

            assertThat(nextNotLeased).isEmpty();
        }

        @Test
        void batchSizeLimits() {
            var state = RECEIVED;
            range(0, 10)
                    .mapToObj(i -> createOffer("id" + i, state))
                    .forEach(getStore()::save);

            // first time works
            var result = getStore().nextNotLeased(3, hasState(state.code()));
            assertThat(result).hasSize(3);
        }

        @Test
        void verifyTemporalOrdering() {
            var state = RECEIVED;
            range(0, 10)
                    .mapToObj(i -> createOffer(String.valueOf(i), state))
                    .peek(this::delayByTenMillis)
                    .forEach(getStore()::save);

            assertThat(getStore().nextNotLeased(20, hasState(state.code())))
                    .extracting(CredentialOffer::getId)
                    .map(Integer::parseInt)
                    .isSortedAccordingTo(Integer::compareTo);
        }

        @Test
        void verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
            var all = range(0, 10)
                    .mapToObj(i -> createOffer("id" + i, RECEIVED))
                    .peek(getStore()::save)
                    .toList();

            Thread.sleep(100);

            var fourth = all.get(3);
            fourth.updateStateTimestamp();
            getStore().save(fourth);

            var next = getStore().nextNotLeased(20, hasState(RECEIVED.code()));
            assertThat(next.indexOf(fourth)).isEqualTo(9);
        }

        @Test
        @DisplayName("Verifies that calling nextNotLeased locks the IP for any subsequent calls")
        void locksEntity() {
            var request = createOffer("id1", RECEIVED);
            getStore().save(request);

            getStore().nextNotLeased(100, hasState(RECEIVED.code()));

            assertThat(isLeasedBy(request.getId(), RUNTIME_ID)).isTrue();
        }

        @Test
        void expiredLease() {
            var request = createOffer("id1", RECEIVED);
            getStore().save(request);

            leaseEntity(request.getId(), RUNTIME_ID, Duration.ofMillis(100));

            await().atLeast(Duration.ofMillis(100))
                    .atMost(getTestTimeout())
                    .until(() -> getStore().nextNotLeased(10, hasState(RECEIVED.code())), hasSize(1));
        }

        @Test
        void shouldLeaseEntityUntilUpdate() {
            var request = createOffer("id1", RECEIVED);
            getStore().save(request);

            var firstQueryResult = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(firstQueryResult).hasSize(1);

            var secondQueryResult = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(secondQueryResult).hasSize(0);

            var retrieved = firstQueryResult.get(0);
            getStore().save(retrieved);

            var thirdQueryResult = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(thirdQueryResult).hasSize(1);
        }

        @Test
        void avoidsStarvation() throws InterruptedException {
            var store = getStore();
            for (var i = 0; i < 10; i++) {
                var process = createOffer("test-request-" + i, RECEIVED);
                store.save(process);
            }

            var list1 = store.nextNotLeased(5, hasState(RECEIVED.code()));
            Thread.sleep(50); //simulate a short delay to generate different timestamps
            list1.forEach(rq -> {
                rq.updateStateTimestamp();
                store.save(rq);
            });
            var list2 = store.nextNotLeased(5, hasState(RECEIVED.code()));
            assertThat(list1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isNotEqualTo(list2)
                    .doesNotContainAnyElementsOf(list2);
        }

        @Test
        void shouldLeaseOrderByStateTimestamp() {

            var all = range(0, 10)
                    .mapToObj(i -> createOffer("id-" + i, RECEIVED))
                    .peek(getStore()::save)
                    .toList();

            all.stream().limit(5)
                    .peek(this::delayByTenMillis)
                    .sorted(Comparator.comparing(CredentialOffer::getStateTimestamp).reversed())
                    .forEach(f -> getStore().save(f));

            var elements = getStore().nextNotLeased(10, hasState(RECEIVED.code()));
            assertThat(elements).hasSize(10).extracting(CredentialOffer::getStateTimestamp).isSorted();
        }

        private void delayByTenMillis(CredentialOffer t) {
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
            var offer = createOfferBuilder().build();
            getStore().save(offer);

            offer.transition(PROCESSED);

            getStore().save(offer);

            assertThat(getStore().query(QuerySpec.none()))
                    .hasSize(1)
                    .first().satisfies(actual -> {
                        assertThat(actual.getState()).isEqualTo(PROCESSED.code());
                    });
        }

        @Test
        @DisplayName("Verify that the lease on a IP is cleared by an update")
        void shouldBreakLease() {
            var offer = createOffer("id1");
            getStore().save(offer);
            // acquire lease
            leaseEntity(offer.getId(), RUNTIME_ID);

            offer.transition(PROCESSED);
            getStore().save(offer);

            // lease should be broken
            var notLeased = getStore().nextNotLeased(10, hasState(PROCESSED.code()));

            assertThat(notLeased).usingRecursiveFieldByFieldElementComparator().containsExactly(offer);
        }

        @Test
        void leasedByOther_shouldThrowException() {
            var id = "id1";
            var offer = createOffer(id);
            getStore().save(offer);
            leaseEntity(id, "someone");

            offer.transition(PROCESSED);

            // leased by someone else -> throw exception
            assertThatThrownBy(() -> getStore().save(offer)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Query {

        @Test
        void noQuerySpec() {
            var all = range(0, 10)
                    .mapToObj(i -> createOffer("id" + i))
                    .peek(getStore()::save)
                    .toList();

            assertThat(getStore().query(QuerySpec.none()))
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrderElementsOf(all);
        }

        @Test
        void verifyFiltering() {
            range(0, 10).forEach(i -> getStore().save(createOffer("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "test-neg-3")).build();

            var result = getStore().query(querySpec);

            assertThat(result).extracting(CredentialOffer::getId).containsOnly("test-neg-3");
        }

        @Test
        void shouldThrowException_whenInvalidOperator() {
            range(0, 10).forEach(i -> getStore().save(createOffer("test-neg-" + i)));
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "foobar", "other")).build();

            assertThatThrownBy(() -> getStore().query(querySpec)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void queryByState() {
            var request = createOffer("testprocess1");
            getStore().save(request);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", request.getState())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(request);
        }

        @Test
        void queryByStateAsString() {
            var offer = createOffer("testprocess1");
            getStore().save(offer);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("state", "=", CredentialOfferStatus.from(offer.getState()).name())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(offer);
        }

        @Test
        void queryByIssuer() {

            range(0, 10)
                    .mapToObj(i -> createOffer("id" + i))
                    .forEach(getStore()::save);

            var offer = createOfferBuilder()
                    .id("testprocess1").issuer("did:web:test").build();

            getStore().save(offer);

            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("issuer", "=", offer.issuer())))
                    .build();

            var result = getStore().query(query);
            assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(offer);
        }

        @Test
        void queryByParticipantContextId() {

            range(0, 10)
                    .mapToObj(i -> createOfferBuilder().id("id" + i).participantContextId("participantContext").build())
                    .forEach(getStore()::save);

            var request = createOfferBuilder()
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
        void verifySorting() {
            range(0, 10).forEach(i -> getStore().save(createOffer("test-neg-" + i)));

            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build()).stream())
                    .hasSize(10)
                    .isSortedAccordingTo(Comparator.comparing(CredentialOffer::getId));
            assertThat(getStore().query(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build()).stream())
                    .hasSize(10)
                    .isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
        }

        @Test
        void verifyPaging() {
            range(0, 10)
                    .mapToObj(i -> createOffer(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
            assertThat(getStore().query(qs)).hasSize(5)
                    .extracting(CredentialOffer::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 8);
        }

        @Test
        void verifyPaging_pageSizeLargerThanCollection() {

            range(0, 10)
                    .mapToObj(i -> createOffer(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
            assertThat(getStore().query(qs))
                    .hasSize(7)
                    .extracting(CredentialOffer::getId)
                    .map(Integer::parseInt)
                    .allMatch(id -> id >= 3 && id < 10);
        }

        @Test
        void verifyPaging_pageSizeOutsideCollection() {

            range(0, 10)
                    .mapToObj(i -> createOffer(String.valueOf(i)))
                    .forEach(getStore()::save);

            var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
            assertThat(getStore().query(qs)).isEmpty();

        }
    }

    @Nested
    class DeleteById {
        @Test
        void deleteById() {
            getStore().save(createOffer("test-delete-id"));
            assertThat(getStore().deleteById("test-delete-id")).isSucceeded();
        }

        @Test
        void deleteById_notFound() {
            assertThat(getStore().deleteById("not-existing-id")).isFailed();
        }
    }

}
