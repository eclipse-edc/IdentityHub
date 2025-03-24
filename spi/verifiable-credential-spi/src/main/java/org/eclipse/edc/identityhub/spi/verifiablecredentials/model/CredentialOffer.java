/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.from;

public class CredentialOffer extends StatefulEntity<CredentialOffer> {
    private List<Object> credentialObjects = new ArrayList<>();
    private String issuer;
    private String participantContextId;

    private CredentialOffer() {

    }

    public String issuer() {
        return issuer;
    }

    public List<Object> getCredentialObjects() {
        return credentialObjects;
    }


    @Override
    public CredentialOffer copy() {
        return CredentialOffer.Builder.newInstance()
                .credentialObjects(List.copyOf(credentialObjects))
                .id(id)
                .participantContextId(participantContextId)
                .clock(clock)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .errorDetail(errorDetail)
                .pending(pending)
                .state(state)
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .traceContext(Map.copyOf(traceContext))
                .issuer(issuer)
                .build();
    }

    @Override
    public String stateAsString() {
        return getStateAsEnum().toString();
    }

    public CredentialOfferStatus getStateAsEnum() {
        return from(state);
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, participantContextId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Entity) o;
        return id.equals(that.getId());
    }

    public void transition(CredentialOfferStatus credentialOfferStatus) {
        state = credentialOfferStatus.code();
    }

    public static final class Builder extends StatefulEntity.Builder<CredentialOffer, CredentialOffer.Builder> {

        Builder(CredentialOffer entity) {
            super(entity);
        }

        public static Builder newInstance() {
            return new Builder(new CredentialOffer());
        }

        @Override
        public Builder id(String id) {
            this.entity.id = id;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder issuer(String issuerDid) {
            this.entity.issuer = issuerDid;
            return this;
        }


        public Builder participantContextId(String participantContextId) {
            this.entity.participantContextId = participantContextId;
            return this;
        }

        public Builder credentialObject(Object credentialObject) {
            this.entity.credentialObjects.add(credentialObject);
            return this;
        }

        public Builder credentialObjects(List<Object> credentialObjects) {
            this.entity.credentialObjects = credentialObjects;
            return this;
        }

        @Override
        public CredentialOffer build() {
            super.build();
            Objects.requireNonNull(entity.issuer, "'issuer' cannot be null");
            Objects.requireNonNull(entity.credentialObjects, "'credentials' cannot be null");
            Objects.requireNonNull(entity.participantContextId, "'participantContextId' cannot be null");

            if (entity.state == 0) {
                throw new IllegalStateException("CredentialOffer state must be set to <> 0");
            }
            return entity;
        }
    }

}
