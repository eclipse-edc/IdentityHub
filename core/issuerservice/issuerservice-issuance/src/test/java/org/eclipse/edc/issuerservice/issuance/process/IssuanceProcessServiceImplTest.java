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

package org.eclipse.edc.issuerservice.issuance.process;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssuanceProcessServiceImplTest {

    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final IssuanceProcessStore store = mock();
    private final IssuanceProcessService service = new IssuanceProcessServiceImpl(transactionContext, store);

    @Test
    void findById_whenFound() {
        var process = createIssuanceProcess();
        var id = "id";
        when(store.findById(id)).thenReturn(process);
        assertThat(service.findById(id)).isSameAs(process);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void findById_whenNotFound() {
        var id = "id";
        assertThat(service.findById(id)).isNull();
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void search() {
        var process = createIssuanceProcess();

        var query = QuerySpec.max();
        when(store.query(query)).thenReturn(Stream.of(process));

        var result = service.search(query);

        assertThat(result).isSucceeded().asInstanceOf(list(IssuanceProcess.class)).containsExactly(process);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    private IssuanceProcess createIssuanceProcess() {

        return IssuanceProcess.Builder.newInstance()
                .id("id")
                .state(IssuanceProcessStates.APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .build();
    }
}
