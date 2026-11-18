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

package org.eclipse.edc.issuerservice.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HolderServiceImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String DID = "did:web:holder";

    private final HolderStore holderStore = mock();
    private final HolderServiceImpl holderService = new HolderServiceImpl(new NoopTransactionContext(), holderStore);

    @Test
    void createHolder() {
        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of()));
        when(holderStore.create(any())).thenReturn(StoreResult.success());
        var holder = holder("holder-id");

        assertThat(holderService.createHolder(holder)).isSucceeded();

        // the DID is only looked for among the holders of the same participant context
        verify(holderStore).query(argThat(query -> filtersBy(query, "participantContextId", PARTICIPANT_CONTEXT_ID) && filtersBy(query, "did", DID)));
        verify(holderStore).create(holder);
    }

    @Test
    void createHolder_whenDidExists_shouldReturnConflict() {
        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(holder("other-holder-id"))));

        assertThat(holderService.createHolder(holder("holder-id"))).isFailed()
                .satisfies(failure -> assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(holderStore, never()).create(any());
    }

    @Test
    void updateHolder_whenDidUnchanged_shouldSucceed() {
        var holder = holder("holder-id");
        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(holder)));
        when(holderStore.update(any())).thenReturn(StoreResult.success());

        assertThat(holderService.updateHolder(holder)).isSucceeded();
        verify(holderStore).update(holder);
    }

    @Test
    void updateHolder_whenDidOfOtherHolder_shouldReturnConflict() {
        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(holder("other-holder-id"))));

        assertThat(holderService.updateHolder(holder("holder-id"))).isFailed()
                .satisfies(failure -> assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT));
        verify(holderStore, never()).update(any());
    }

    @Test
    void patchHolder() {
        var existing = holderBuilder("holder-id").holderName("holder name").isAnonymous(true).createdAt(1000L).lastModifiedAt(1000L)
                .property("role", "member").property("region", "eu").build();
        when(holderStore.findById(PARTICIPANT_CONTEXT_ID, "holder-id")).thenReturn(StoreResult.success(existing));
        when(holderStore.update(any())).thenReturn(StoreResult.success());

        var result = holderService.patchHolder(PARTICIPANT_CONTEXT_ID, "holder-id", DID, "holder name", Map.of("role", "operator", "tier", "gold"), Set.of("region", "unknown"));

        assertThat(result).isSucceeded().satisfies(patched -> {
            assertThat(patched.getProperties()).containsOnly(Map.entry("role", "operator"), Map.entry("tier", "gold"));
            assertThat(patched.getDid()).isEqualTo(DID);
            assertThat(patched.getHolderName()).isEqualTo("holder name");
            assertThat(patched.isAnonymous()).isTrue();
            assertThat(patched.getCreatedAt()).isEqualTo(1000L);
            assertThat(patched.getLastModifiedAt()).isGreaterThan(1000L);
        });
        verify(holderStore).update(existing);
    }

    @Test
    void patchHolder_whenNotFound_shouldReturnNotFound() {
        when(holderStore.findById(any(), any())).thenReturn(StoreResult.notFound("not found"));

        assertThat(holderService.patchHolder(PARTICIPANT_CONTEXT_ID, "holder-id", DID, null, Map.of(), Set.of())).isFailed()
                .satisfies(failure -> assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND));
        verify(holderStore, never()).update(any());
    }

    @Test
    void patchHolder_whenDidDoesNotMatch_shouldReturnBadRequest() {
        when(holderStore.findById(any(), any())).thenReturn(StoreResult.success(holder("holder-id")));

        assertThat(holderService.patchHolder(PARTICIPANT_CONTEXT_ID, "holder-id", "did:web:other", null, Map.of("foo", "bar"), Set.of())).isFailed()
                .satisfies(failure -> assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST));
        verify(holderStore, never()).update(any());
    }

    @Test
    void patchHolder_whenNameDoesNotMatch_shouldReturnBadRequest() {
        when(holderStore.findById(any(), any())).thenReturn(StoreResult.success(holderBuilder("holder-id").holderName("holder name").build()));

        assertThat(holderService.patchHolder(PARTICIPANT_CONTEXT_ID, "holder-id", DID, "other name", Map.of("foo", "bar"), Set.of())).isFailed()
                .satisfies(failure -> assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST));
        verify(holderStore, never()).update(any());
    }

    private static boolean filtersBy(QuerySpec query, String property, String value) {
        return query.getFilterExpression().stream()
                .anyMatch(criterion -> property.equals(criterion.getOperandLeft()) && value.equals(criterion.getOperandRight()));
    }

    private static Holder holder(String holderId) {
        return holderBuilder(holderId).build();
    }

    private static Holder.Builder holderBuilder(String holderId) {
        return Holder.Builder.newInstance()
                .holderId(holderId)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .did(DID);
    }
}
