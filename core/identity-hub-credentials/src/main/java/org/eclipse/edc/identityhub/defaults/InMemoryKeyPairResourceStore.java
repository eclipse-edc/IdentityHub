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
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.connector.core.store.CriterionToPredicateConverterImpl;
import org.eclipse.edc.connector.core.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.store.model.KeyPairResource;
import org.eclipse.edc.spi.query.QueryResolver;

public class InMemoryKeyPairResourceStore extends InMemoryEntityStore<KeyPairResource> implements KeyPairResourceStore {
    @Override
    protected String getId(KeyPairResource newObject) {
        return newObject.getId();
    }

    @Override
    protected QueryResolver<KeyPairResource> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(KeyPairResource.class, new CriterionToPredicateConverterImpl());

    }
}
