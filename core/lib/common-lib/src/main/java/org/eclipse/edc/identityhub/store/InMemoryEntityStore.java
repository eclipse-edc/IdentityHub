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

package org.eclipse.edc.identityhub.store;

import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

/**
 * Base class for in-mem entity stores, that implement basic CRUD operations.
 */
public abstract class InMemoryEntityStore<T> {
    protected final Map<String, T> store = new HashMap<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    protected final QueryResolver<T> queryResolver;
    protected final CriterionOperatorRegistry criterionOperatorRegistry;

    protected InMemoryEntityStore() {
        criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();
        queryResolver = createQueryResolver();
    }


    public StoreResult<T> findById(String id) {
        lock.readLock().lock();
        try {
            var result = store.get(id);
            return result == null ? notFound("An entity with ID '%s' does not exist.".formatted(id)) : success(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Creates a new entity if none exists.
     *
     * @param newObject the new object to insert.
     * @return failure if an object with the same ID already exists.
     */
    public StoreResult<Void> create(T newObject) {
        lock.writeLock().lock();
        var id = getId(newObject);
        try {
            if (store.containsKey(id)) {
                return alreadyExists("An entity with ID %s already exists".formatted(id));
            }
            store.put(id, newObject);
            return success(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Performs a query using the given query parameters.
     *
     * @param querySpec A non-null QuerySpec.
     * @return A (potentially empty) Stream of objects. Callers must close the stream.
     */
    public StoreResult<Collection<T>> query(QuerySpec querySpec) {
        lock.readLock().lock();
        try {
            // if no filter is present, we return true
            var result = queryResolver.query(store.values().stream(), querySpec, Predicate::and, x -> true);
            return success(result.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Replaces an existing entity with a new object.
     *
     * @param newObject the new entity
     * @return failure if an object with the same ID was not found.
     */
    public StoreResult<Void> update(T newObject) {
        lock.writeLock().lock();
        try {
            var id = getId(newObject);
            if (!store.containsKey(id)) {
                return notFound("An entity with ID '%s' does not exist.".formatted(id));
            }
            store.put(id, newObject);
            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes the object with the given ID
     *
     * @param id The ID of the object to delete.
     * @return failure if an object with the given ID was not found.
     */
    public StoreResult<Void> deleteById(String id) {
        lock.writeLock().lock();
        try {
            if (!store.containsKey(id)) {
                return notFound("An entity with ID '%s' does not exist.".formatted(id));
            }
            store.remove(id);
            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected abstract String getId(T newObject);

    protected abstract QueryResolver<T> createQueryResolver();
}
