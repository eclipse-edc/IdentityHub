/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store;

import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.junit.jupiter.api.BeforeEach;

class InMemoryIdentityHubStoreTest extends IdentityHubStoreTestBase {

    private InMemoryIdentityHubStore store;

    @BeforeEach
    void setup() {
        store = new InMemoryIdentityHubStore();
    }

    @Override
    protected IdentityHubStore getStore() {
        return store;
    }
}
