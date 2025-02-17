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

package org.eclipse.edc.identityhub.spi.credential.request.model;

import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.from;

/**
 * This represents the ongoing credential request on the holder (= IdentityHub) side. From the moment a credential request
 * is made via the IdentityApi, until the time the credential is issued by the Issuer, the {@link HolderCredentialRequest}
 * can be inspected to see the current status.
 * <p>
 * Note: This is the holder-side equivalent of the issuer's {@code IssuanceProcess}
 */
public class HolderCredentialRequest extends StatefulEntity<HolderCredentialRequest> {

    private String participantContextId;
    private String issuerDid;
    private List<String> credentialTypes = new ArrayList<>();

    private HolderCredentialRequest() {
        this.state = HolderRequestState.CREATED.code();
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    /**
     * This is the unique ID of this request. Identical to {@link HolderCredentialRequest#getId()}
     */
    public String getRequestId() {
        return getId();
    }

    public List<String> getCredentialTypes() {
        return credentialTypes;
    }

    @Override
    public HolderCredentialRequest copy() {
        return Builder.newInstance()
                .state(state)
                .id(id)
                .credentialTypes(List.copyOf(credentialTypes))
                .issuerDid(issuerDid)
                .errorDetail(errorDetail)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .pending(pending)
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .clock(clock)
                .traceContext(Map.copyOf(traceContext))
                .build();
    }

    @Override
    public String stateAsString() {
        return from(state).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends StatefulEntity.Builder<HolderCredentialRequest, Builder> {

        Builder(HolderCredentialRequest entity) {
            super(entity);
        }

        public static Builder newInstance() {
            return new Builder(new HolderCredentialRequest());
        }

        public Builder id(String id) {
            this.entity.id = id;
            return this;
        }

        public Builder issuerDid(String issuerDid) {
            this.entity.issuerDid = issuerDid;
            return this;
        }

        public Builder requestId(String requestId) {
            this.entity.id = requestId;
            return this;
        }

        public Builder credentialTypes(List<String> credentialTypes) {
            this.entity.credentialTypes = credentialTypes;
            return this;
        }

        public Builder credentialType(String credentialTypes) {
            this.entity.credentialTypes.add(credentialTypes);
            return this;
        }

        public Builder participantContext(String participantContextId) {
            this.entity.participantContextId = participantContextId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public HolderCredentialRequest build() {
            super.build();
            Objects.requireNonNull(entity.issuerDid, "'issuerDid' cannot be null");
            Objects.requireNonNull(entity.credentialTypes, "'credentialTypes' cannot be null");
            Objects.requireNonNull(entity.participantContextId, "'participantContextId' cannot be null");
            if (entity.credentialTypes.isEmpty()) {
                throw new IllegalArgumentException("CredentialTypes cannot be empty");
            }

            if (entity.state == 0) {
                throw new IllegalStateException("Issuance process state must be set to <> 0");
            }
            return entity;
        }
    }

}
