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

import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcessStates.from;


/**
 * Tracks credential issuance on the Issuer side.
 * <p>
 * A credential issuance process is created in response to an issuance request by a holder and involves generating one or more credentials.
 * The credentials are defined by a {@link CredentialDefinition}, which specifies claim attestations, rules which must be satisfied,
 * and mappings from claim data to credential data. When a request is received, attestations are sourced and rules are verified.
 * If successful, an issuance process is created with claims gathered from attestations. The issuance process is then approved
 * asynchronously and generated credentials sent to the holder.
 */
public class IssuerCredentialIssuanceProcess extends StatefulEntity<IssuerCredentialIssuanceProcess> {
    private final Map<String, Object> claims = new HashMap<>();
    private final List<String> credentialDefinitions = new ArrayList<>();
    private String participantId;

    private IssuerCredentialIssuanceProcess() {
    }

    @Override
    public IssuerCredentialIssuanceProcess copy() {
        var builder = Builder.newInstance()
                .claims(claims)
                .credentialDefinitions(credentialDefinitions)
                .participantId(participantId);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return from(state).name();
    }

    public String getParticipantId() {
        return participantId;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public List<String> getCredentialDefinitions() {
        return credentialDefinitions;
    }

    public Builder toBuilder() {
        return new Builder(this);
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
        var that = (IssuerCredentialIssuanceProcess) o;
        return id.equals(that.id);
    }

    public static final class Builder extends StatefulEntity.Builder<IssuerCredentialIssuanceProcess, Builder> {

        private Builder(IssuerCredentialIssuanceProcess process) {
            super(process);
        }

        public static Builder newInstance() {
            return new Builder(new IssuerCredentialIssuanceProcess());
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

        public Builder participantId(String participantId) {
            this.entity.participantId = participantId;
            return this;
        }

        @Override
        public IssuerCredentialIssuanceProcess build() {
            super.build();

            if (entity.state == 0) {
                throw new IllegalStateException("Issuance process state must be set");
            }
            Objects.requireNonNull(entity.participantId, "Participant ID must be set");
            return entity;
        }

    }

}
