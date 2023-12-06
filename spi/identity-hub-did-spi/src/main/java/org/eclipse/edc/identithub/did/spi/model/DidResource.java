/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.did.spi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a {@link org.eclipse.edc.iam.did.spi.document.DidDocument}
 */
public class DidResource {
    @JsonIgnore
    private Clock clock = Clock.systemUTC();
    private String did;
    private DidState state = DidState.INITIAL;
    private long stateTimestamp;
    private long createTimestamp;
    private List<Service> serviceEndpoints = new ArrayList<>();
    private List<VerificationMethod> verificationMethods = new ArrayList<>();
    // todo: what is this?
    // private List<VerificationRelationship> verificationRelationships;

    private DidResource() {
    }

    public String getDid() {
        return did;
    }

    public DidState getState() {
        return state;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public List<Service> getServiceEndpoints() {
        return serviceEndpoints;
    }

    public List<VerificationMethod> getVerificationMethods() {
        return verificationMethods;
    }

    public static final class Builder {
        private final DidResource resource;

        private Builder() {
            resource = new DidResource();
        }

        public Builder did(String did) {
            this.resource.did = did;
            return this;
        }

        public Builder state(DidState state) {
            this.resource.state = state;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.resource.stateTimestamp = timestamp;
            return this;
        }

        public Builder clock(Clock clock) {
            this.resource.clock = clock;
            return this;
        }

        public Builder serviceEndpoints(List<Service> serviceEndpoints) {
            this.resource.serviceEndpoints = serviceEndpoints;
            return this;
        }

        public Builder serviceEndpoint(Service service) {
            this.resource.serviceEndpoints.add(service);
            return this;
        }

        public Builder verificationMethods(List<VerificationMethod> verificationMethodResources) {
            this.resource.verificationMethods = verificationMethodResources;
            return this;
        }

        public Builder verificationMethod(VerificationMethod method) {
            this.resource.verificationMethods.add(method);
            return this;
        }

        public DidResource build() {
            Objects.requireNonNull(resource.did, "Must have an identifier");
            Objects.requireNonNull(resource.state, "Must have a state");

            if (resource.stateTimestamp <= 0) {
                resource.stateTimestamp = resource.clock.millis();
            }

            return resource;
        }

        public static Builder newInstance() {
            return new Builder();
        }


    }
}
