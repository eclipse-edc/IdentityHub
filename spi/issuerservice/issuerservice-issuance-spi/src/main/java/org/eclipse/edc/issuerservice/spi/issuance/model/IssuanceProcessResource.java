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

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Objects;

/**
 * Implementation of a {@link ParticipantResource} for an {@link IssuanceProcess} for integrating with
 * the Identity Hub AuthorizationService.
 */
public class IssuanceProcessResource extends ParticipantResource {

    private IssuanceProcess issuanceProcess;

    public static QuerySpec.Builder queryByIssuerContextId(String issuerContextId) {
        return QuerySpec.Builder.newInstance().filter(filterByIssuerContextId(issuerContextId));
    }

    public static Criterion filterByIssuerContextId(String issuerContextId) {
        return new Criterion("issuerContextId", "=", issuerContextId);
    }

    public static final class Builder extends ParticipantResource.Builder<IssuanceProcessResource, IssuanceProcessResource.Builder> {

        private Builder() {
            super(new IssuanceProcessResource());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder issuanceProcess(IssuanceProcess issuanceProcess) {
            this.entity.issuanceProcess = issuanceProcess;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IssuanceProcessResource build() {
            Objects.requireNonNull(entity.issuanceProcess, "issuanceProcess cannot be null");
            return super.build();
        }

    }
}
