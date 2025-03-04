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

import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStoreTestBase;

class InMemoryHolderStoreTest extends HolderStoreTestBase {

    private final InMemoryHolderStore store = new InMemoryHolderStore();

    @Override
    protected HolderStore getStore() {
        return store;
    }
}