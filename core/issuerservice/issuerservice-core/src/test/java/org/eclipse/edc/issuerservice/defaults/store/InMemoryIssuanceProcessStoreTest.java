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

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

import java.time.Duration;

public class InMemoryIssuanceProcessStoreTest extends IssuanceProcessStoreTestBase {

    private final InMemoryIssuanceProcessStore store = new InMemoryIssuanceProcessStore(IssuanceProcessStoreTestBase.RUNTIME_ID, clock, CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected IssuanceProcessStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String issuanceId, String owner, Duration duration) {
        store.acquireLease(issuanceId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String issuanceId, String owner) {
        return store.isLeasedBy(issuanceId, owner);
    }
}
