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

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

public class InMemoryKeyPairResourceStore extends InMemoryEntityStore<KeyPairResource> implements KeyPairResourceStore {
    @Override
    protected String getId(KeyPairResource newObject) {
        return newObject.getId();
    }

    @Override
    protected QueryResolver<KeyPairResource> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(KeyPairResource.class, criterionOperatorRegistry);

    }
}
