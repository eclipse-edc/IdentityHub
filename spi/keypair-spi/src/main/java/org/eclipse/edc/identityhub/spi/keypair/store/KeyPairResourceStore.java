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

package org.eclipse.edc.identityhub.spi.keypair.store;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;


/**
 * Stores {@link KeyPairResource} entities in persistent storage.
 */
public interface KeyPairResourceStore {

    /**
     * Create a new entry in the database using the data specified.
     *
     * @param keyPairResource the new KeyPairResource. Must not already exist.
     * @return failure if a KeyPairResource with the same ID already exists.
     */
    StoreResult<Void> create(KeyPairResource keyPairResource);

    /**
     * Searches for KeyPairResources using the given {@link QuerySpec}.
     *
     * @param query the query
     * @return A (potentially) empty collection of key pair resources, never null.
     */
    StoreResult<Collection<KeyPairResource>> query(QuerySpec query);

    /**
     * Updates a given KeyPairResource.
     *
     * @param keyPairResource The updated KeyPairResource. Will overwrite existing data in the database.
     * @return failure if a KeyPairResource with the same ID does not yet exist.
     */
    StoreResult<Void> update(KeyPairResource keyPairResource);

    /**
     * Deletes a KeyPairResource with the given ID, if it exists.
     *
     * @param id The id of the KeyPairResource to delete
     * @return failure if the specified KeyPairResource does not exist.
     */
    StoreResult<Void> deleteById(String id);
}
