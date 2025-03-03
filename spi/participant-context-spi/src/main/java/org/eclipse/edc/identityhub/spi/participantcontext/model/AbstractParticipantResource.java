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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

/**
 * This is the base class for all resources that are owned by a {@link ParticipantContext}.
 */
public abstract class AbstractParticipantResource implements ParticipantResource {
    protected String participantContextId;


    /**
     * The {@link ParticipantContext} that this resource belongs to.
     */
    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    public abstract static class Builder<T extends AbstractParticipantResource, B extends AbstractParticipantResource.Builder<T, B>> {
        protected final T entity;

        protected Builder(T entity) {
            this.entity = entity;
        }

        public abstract B self();

        public B participantContextId(String participantContextId) {
            entity.participantContextId = participantContextId;
            return self();
        }

        protected T build() {
            return entity;
        }
    }
}