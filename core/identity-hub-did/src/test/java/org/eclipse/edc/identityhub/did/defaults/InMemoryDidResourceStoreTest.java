/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.did.defaults;

import org.eclipse.edc.identityhub.did.store.test.DidResourceStoreTestBase;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryDidResourceStoreTest extends DidResourceStoreTestBase {

    private final DidResourceStore store = new InMemoryDidResourceStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected DidResourceStore getStore() {
        return store;
    }
}