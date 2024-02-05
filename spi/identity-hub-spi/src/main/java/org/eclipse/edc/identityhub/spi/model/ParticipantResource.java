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

package org.eclipse.edc.identityhub.spi.model;

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

/**
 * This is the base class for all resources that are owned by a {@link ParticipantContext}.
 */
public abstract class ParticipantResource {
    protected String participantId;

    /**
     * The {@link ParticipantContext} that this resource belongs to.
     */

    public String getParticipantId() {
        return participantId;
    }

    public static QuerySpec.Builder queryByParticipantId(String participantId) {
        return QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", participantId));
    }

    public abstract static class Builder<T extends ParticipantResource, B extends ParticipantResource.Builder<T, B>> {
        protected final T entity;

        protected Builder(T entity) {
            this.entity = entity;
        }

        public abstract B self();

        public B participantId(String participantId) {
            entity.participantId = participantId;
            return self();
        }

        protected T build() {
            return entity;
        }
    }
}