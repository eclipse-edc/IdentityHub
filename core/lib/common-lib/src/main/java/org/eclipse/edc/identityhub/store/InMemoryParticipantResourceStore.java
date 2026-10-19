/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.store;

import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.result.StoreResult;

/**
 * Base class for in-mem stores of entities whose ID is only unique within their participant context. An entity is
 * identified by its participant context ID and its ID, so two participant contexts may use the same ID.
 */
public abstract class InMemoryParticipantResourceStore<T extends ParticipantResource> extends InMemoryEntityStore<T> {

    public StoreResult<T> findById(String participantContextId, String id) {
        var result = findById(participantScopedKey(participantContextId, id));
        return result.succeeded() ? result : StoreResult.notFound(notFoundMessage(id));
    }

    public StoreResult<Void> deleteById(String participantContextId, String id) {
        var result = deleteById(participantScopedKey(participantContextId, id));
        return result.succeeded() ? result : StoreResult.notFound(notFoundMessage(id));
    }

    @Override
    public StoreResult<Void> create(T newObject) {
        var result = super.create(newObject);
        return result.succeeded() ? result : StoreResult.alreadyExists(alreadyExistsMessage(getResourceId(newObject)));
    }

    @Override
    public StoreResult<Void> update(T newObject) {
        var result = super.update(newObject);
        return result.succeeded() ? result : StoreResult.notFound(notFoundMessage(getResourceId(newObject)));
    }

    @Override
    protected String getId(T newObject) {
        return participantScopedKey(newObject.getParticipantContextId(), getResourceId(newObject));
    }

    /**
     * The ID of the entity, which is unique within its participant context.
     */
    protected abstract String getResourceId(T entity);

    protected abstract String notFoundMessage(String id);

    protected abstract String alreadyExistsMessage(String id);

    /**
     * The key under which an entity is kept. The length prefix keeps the key unambiguous whatever characters the
     * participant context ID contains.
     */
    private static String participantScopedKey(String participantContextId, String id) {
        return participantContextId.length() + ":" + participantContextId + id;
    }
}
