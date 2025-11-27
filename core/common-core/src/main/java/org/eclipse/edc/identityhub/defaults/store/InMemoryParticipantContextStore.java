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

import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

/**
 * In-memory variant of the {@link ParticipantContextStore} that is thread-safe.
 */
public class InMemoryParticipantContextStore extends InMemoryEntityStore<IdentityHubParticipantContext> implements ParticipantContextStore {
    @Override
    protected String getId(IdentityHubParticipantContext newObject) {
        return newObject.getParticipantContextId();
    }

    @Override
    protected QueryResolver<IdentityHubParticipantContext> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(IdentityHubParticipantContext.class, criterionOperatorRegistry);
    }
}
