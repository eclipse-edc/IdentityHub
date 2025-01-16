/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.defaults.store;

import org.eclipse.edc.identityhub.keypair.store.KeyPairResourceStoreTestBase;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;

class InMemoryKeyPairResourceStoreTest extends KeyPairResourceStoreTestBase {

    private final InMemoryKeyPairResourceStore store = new InMemoryKeyPairResourceStore();

    @Override
    protected KeyPairResourceStore getStore() {
        return store;
    }
}