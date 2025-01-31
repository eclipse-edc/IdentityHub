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

import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.issuerservice.spi.participant.models.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

/**
 * Purely in-memory implementation of the {@link Participant} store.
 */
public class InMemoryParticipantStore extends InMemoryEntityStore<Participant> implements ParticipantStore {
    @Override
    protected String getId(Participant newObject) {
        return newObject.participantId();
    }

    @Override
    protected QueryResolver<Participant> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(Participant.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Participant> findById(String id) {
        return null;
    }
}
