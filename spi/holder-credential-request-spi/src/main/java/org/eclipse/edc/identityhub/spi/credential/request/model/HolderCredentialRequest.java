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

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
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
    private List<RequestedCredential> idsAndFormats = new ArrayList<>();
    private String issuerPid;

    private HolderCredentialRequest() {
        this.state = CREATED.code();
    }

    public void transitionCreated() {
        state = CREATED.code();
        updateStateTimestamp();
    }

    public void transitionRequesting() {
        state = REQUESTING.code();
        updateStateTimestamp();
    }

    public void transitionRequested(String issuerPid) {
        state = REQUESTED.code();
        this.issuerPid = issuerPid;
        updateStateTimestamp();
    }

    public void transitionIssued(String issuerPid) {
        state = ISSUED.code();
        this.issuerPid = issuerPid;
        updateStateTimestamp();
    }

    public void transitionError(String detail) {
        state = ERROR.code();
        errorDetail = detail;
        updateStateTimestamp();
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    /**
     * This is the unique ID of this request on the holder side.. Identical to {@link HolderCredentialRequest#getId()}
     */
    public String getHolderPid() {
        return getId();
    }

    public List<RequestedCredential> getIdsAndFormats() {
        return idsAndFormats;
    }

    @Override
    public HolderCredentialRequest copy() {
        return Builder.newInstance()
                .state(state)
                .id(id)
                .requestedCredentials(List.copyOf(idsAndFormats))
                .issuerDid(issuerDid)
                .participantContextId(participantContextId)
                .issuerPid(issuerPid)
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

    public HolderRequestState stateAsEnum() {
        return from(state);
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

    /**
     * The process ID that the issuer returned in the response to the credential request. Note that this is <strong>not</strong>
     * the ID assigned by the holder, when <em>making</em> the request! This ID is needed for status inquiries, etc.
     */
    public String getIssuerPid() {
        return issuerPid;
    }

    public static final class Builder extends StatefulEntity.Builder<HolderCredentialRequest, Builder> {

        Builder(HolderCredentialRequest entity) {
            super(entity);
        }

        public static Builder newInstance() {
            return new Builder(new HolderCredentialRequest());
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

        public Builder issuerDid(String issuerDid) {
            this.entity.issuerDid = issuerDid;
            return this;
        }

        public Builder requestId(String requestId) {
            this.entity.id = requestId;
            return this;
        }

        public Builder requestedCredentials(List<RequestedCredential> typesAndFormats) {
            this.entity.idsAndFormats = typesAndFormats;
            return this;
        }

        public Builder requestedCredential(String credentialObjectId, String type, String format) {
            this.entity.idsAndFormats.add(new RequestedCredential(credentialObjectId, type, format));
            return this;
        }

        public Builder participantContextId(String participantContextId) {
            this.entity.participantContextId = participantContextId;
            return this;
        }

        public Builder issuerPid(String issuerPid) {
            this.entity.issuerPid = issuerPid;
            return this;
        }

        @Override
        public HolderCredentialRequest build() {
            super.build();
            Objects.requireNonNull(entity.issuerDid, "'issuerDid' cannot be null");
            Objects.requireNonNull(entity.idsAndFormats, "'idsAndFormats' cannot be null");
            Objects.requireNonNull(entity.participantContextId, "'participantContextId' cannot be null");
            if (entity.idsAndFormats.isEmpty()) {
                throw new IllegalArgumentException("idsAndFormats cannot be empty");
            }

            if (entity.state == 0) {
                throw new IllegalStateException("Issuance process state must be set to <> 0");
            }
            return entity;
        }
    }


}
