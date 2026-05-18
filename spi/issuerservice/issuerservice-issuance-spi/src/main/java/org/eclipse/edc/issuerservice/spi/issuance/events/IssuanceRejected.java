/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.spi.issuance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class IssuanceRejected extends IssuanceEvent {
    private String reason;

    public String getReason() {
        return reason;
    }

    @Override
    public String name() {
        return "issuance.rejected";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends IssuanceEvent.Builder<IssuanceRejected, IssuanceRejected.Builder> {

        protected Builder() {
            super(new IssuanceRejected());
        }

        @JsonCreator
        public static IssuanceRejected.Builder newInstance() {
            return new IssuanceRejected.Builder();
        }


        @Override
        public IssuanceRejected.Builder self() {
            return this;
        }

        public <B> Builder reason(String failureDetail) {
            event.reason = failureDetail;
            return self();
        }
    }
}
