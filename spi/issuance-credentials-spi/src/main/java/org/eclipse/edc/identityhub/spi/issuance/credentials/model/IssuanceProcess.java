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

package org.eclipse.edc.identityhub.spi.issuance.credentials.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Tracks credential issuance.
 * <p>
 * An issuance process is created in response to an issuance request by a holder and involves generating one or more credentials.
 * The credentials are defined by a {@link CredentialDefinition}, which specifies claim attestations, rules which must be satisfied,
 * and mappings from claim data to credential data. When a request is received, attestations are sourced and rules are verified.
 * If successful, an issuance process is created with claims gathered from attestations. The issuance process is then approved
 * asynchronously and generated credentials sent to the holder.
 */
public class IssuanceProcess {
    public enum State {
        SUBMITTED, APPROVED, DELIVERED, ERRORED
    }

    private String id;
    private State state = State.SUBMITTED;
    private long stateTimestamp;
    private int retries;
    private int errorCode;

    private long creationTime;

    private Map<String, Object> claims;
    private List<String> credentialDefinitions;

    public State getState() {
        return state;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public int getRetries() {
        return retries;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public List<String> getCredentialDefinitions() {
        return credentialDefinitions;
    }

    private IssuanceProcess() {
    }

    public static final class Builder {
        private IssuanceProcess process;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.process.id = id;
            return this;
        }

        public Builder state(State state) {
            this.process.state = state;
            return this;
        }

        public Builder stateTimestamp(long timestamp) {
            this.process.stateTimestamp = timestamp;
            return this;
        }

        public Builder retries(int retries) {
            this.process.retries = retries;
            return this;
        }

        public Builder errorCode(int errorCode) {
            this.process.errorCode = errorCode;
            return this;
        }

        public Builder creationTime(int creationTime) {
            this.process.creationTime = creationTime;
            return this;
        }

        public Builder claims(Map<String, Object> claims) {
            this.process.claims.putAll(claims);
            return this;
        }

        public Builder credentialDefinitions(Collection<String> definitions) {
            this.process.credentialDefinitions.addAll(definitions);
            return this;
        }

        public Builder credentialDefinitions(String id) {
            this.process.credentialDefinitions.add(id);
            return this;
        }

        public IssuanceProcess build() {
            requireNonNull(process.id, "id");
            return process;
        }

        private Builder() {
            process = new IssuanceProcess();
        }

    }

}
