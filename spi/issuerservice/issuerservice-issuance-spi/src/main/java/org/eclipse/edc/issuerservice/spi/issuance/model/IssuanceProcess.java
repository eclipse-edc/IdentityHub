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

package org.eclipse.edc.issuerservice.spi.issuance.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.APPROVED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.DELIVERED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.ERRORED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.from;


/**
 * Tracks credential issuance.
 * <p>
 * An issuance process is created in response to an issuance request by a holder and involves generating one or more credentials.
 * The credentials are defined by a {@link CredentialDefinition}, which specifies claim attestations, rules which must be satisfied,
 * and mappings from claim data to credential data. When a request is received, attestations are sourced and rules are verified.
 * If successful, an issuance process is created with claims gathered from attestations. The issuance process is then approved
 * asynchronously and generated credentials sent to the holder.
 */
public class IssuanceProcess extends StatefulEntity<IssuanceProcess> implements ParticipantResource {
    private final Map<String, Object> claims = new HashMap<>();
    private final List<String> credentialDefinitions = new ArrayList<>();
    private final Map<String, CredentialFormat> credentialFormats = new HashMap<>();
    private String holderId;
    private String participantContextId;
    private String holderPid;

    private IssuanceProcess() {
    }

    @Override
    public IssuanceProcess copy() {
        var builder = Builder.newInstance()
                .claims(claims)
                .credentialDefinitions(credentialDefinitions)
                .holderId(holderId)
                .credentialFormats(credentialFormats)
                .participantContextId(participantContextId)
                .holderPid(holderPid);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return from(state).name();
    }

    public String getHolderId() {
        return holderId;
    }

    public String getHolderPid() {
        return holderPid;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public List<String> getCredentialDefinitions() {
        return credentialDefinitions;
    }

    public Map<String, CredentialFormat> getCredentialFormats() {
        return credentialFormats;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public void transitionToDelivered() {
        transition(DELIVERED, APPROVED);
    }

    public void transitionToApproved() {
        transition(APPROVED, APPROVED);
    }

    public void transitionToError() {
        transition(ERRORED, APPROVED);
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
        var that = (IssuanceProcess) o;
        return id.equals(that.id);
    }

    private void transition(IssuanceProcessStates end, IssuanceProcessStates... starts) {
        transition(end, (state) -> Arrays.stream(starts).anyMatch(s -> s == state));
    }

    /**
     * Transition to a given end state if the passed predicate test correctly. Increases the
     * state count if transitioned to the same state and updates the state timestamp.
     *
     * @param end          The desired state.
     * @param canTransitTo Tells if the issuance process can transit to that state.
     */
    private void transition(IssuanceProcessStates end, Predicate<IssuanceProcessStates> canTransitTo) {
        var targetState = end.code();
        if (!canTransitTo.test(IssuanceProcessStates.from(state))) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", IssuanceProcessStates.from(state), IssuanceProcessStates.from(targetState)));
        }
        transitionTo(targetState);
    }

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    public static final class Builder extends StatefulEntity.Builder<IssuanceProcess, Builder> {

        private Builder(IssuanceProcess process) {
            super(process);
        }

        public static Builder newInstance() {
            return new Builder(new IssuanceProcess());
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder claims(Map<String, Object> claims) {
            this.entity.claims.putAll(claims);
            return this;
        }

        public Builder credentialDefinitions(Collection<String> definitions) {
            this.entity.credentialDefinitions.addAll(definitions);
            return this;
        }

        public Builder credentialDefinitions(String id) {
            this.entity.credentialDefinitions.add(id);
            return this;
        }

        public Builder credentialFormats(Map<String, CredentialFormat> formats) {
            this.entity.credentialFormats.putAll(formats);
            return this;
        }

        public Builder holderId(String holderId) {
            this.entity.holderId = holderId;
            return this;
        }

        public Builder participantContextId(String participantContextId) {
            this.entity.participantContextId = participantContextId;
            return this;
        }

        public Builder holderPid(String holderPid) {
            this.entity.holderPid = holderPid;
            return this;
        }

        @Override
        public IssuanceProcess build() {
            super.build();

            if (entity.state == 0) {
                throw new IllegalStateException("Issuance process state must be set");
            }
            Objects.requireNonNull(entity.holderId, "Member ID must be set");
            Objects.requireNonNull(entity.participantContextId, "Participant Context ID must be set");
            Objects.requireNonNull(entity.holderPid, "Holder Pid must be set");
            return entity;
        }

    }

}
