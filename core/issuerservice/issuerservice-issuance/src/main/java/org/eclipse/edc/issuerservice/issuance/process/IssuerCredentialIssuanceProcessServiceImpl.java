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

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuerCredentialIssuanceProcessService;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

public class IssuerCredentialIssuanceProcessServiceImpl implements IssuerCredentialIssuanceProcessService {

    private final TransactionContext transactionContext;
    private final IssuanceProcessStore issuanceProcessStore;

    public IssuerCredentialIssuanceProcessServiceImpl(TransactionContext transactionContext, IssuanceProcessStore issuanceProcessStore) {
        this.transactionContext = transactionContext;
        this.issuanceProcessStore = issuanceProcessStore;
    }

    @Override
    public IssuerCredentialIssuanceProcess findById(String id) {
        return transactionContext.execute(() -> issuanceProcessStore.findById(id));
    }

    @Override
    public ServiceResult<List<IssuerCredentialIssuanceProcess>> search(QuerySpec query) {
        return ServiceResult.success(queryIssuanceProcesses(query));
    }

    private List<IssuerCredentialIssuanceProcess> queryIssuanceProcesses(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = issuanceProcessStore.query(query)) {
                return stream.toList();
            }
        });
    }
}
