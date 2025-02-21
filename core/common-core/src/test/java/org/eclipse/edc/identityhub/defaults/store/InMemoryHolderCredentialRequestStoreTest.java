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

package org.eclipse.edc.identityhub.defaults.store;

import org.eclipse.edc.identityhub.credential.request.test.HolderCredentialRequestStoreTestBase;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

import java.time.Duration;

class InMemoryHolderCredentialRequestStoreTest extends HolderCredentialRequestStoreTestBase {

    private final InMemoryHolderCredentialRequestStore store = new InMemoryHolderCredentialRequestStore(RUNTIME_ID, clock, CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected HolderCredentialRequestStore getStore() {
        return store;
    }

    @Override
    protected boolean isLeasedBy(String issuanceId, String owner) {
        return store.isLeasedBy(issuanceId, owner);
    }

    @Override
    protected void leaseEntity(String holderPid, String owner, Duration duration) {
        store.acquireLease(holderPid, owner, duration);
    }
}