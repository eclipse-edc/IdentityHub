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


import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

public abstract class IssuanceEvent extends Event {

    protected String holderId;
    protected String issuerParticipantContextId;
    protected String holderProcessId;
    protected String issuanceProcessId;

    /**
     * The ID of the holder that requested the credential.
     */
    public String getHolderId() {
        return holderId;
    }

    /**
     * Retrieves the ID of the issuer participant context.
     */
    public String getIssuerParticipantContextId() {
        return issuerParticipantContextId;
    }

    /**
     * Retrieves the ID that the holder has assigned to the issuance process.
     */
    public String getHolderProcessId() {
        return holderProcessId;
    }

    /**
     * Retrieves the ID of the issuance process.
     */
    public String getIssuanceProcessId() {
        return issuanceProcessId;
    }

    public abstract static class Builder<T extends IssuanceEvent, B extends IssuanceEvent.Builder<T, B>> {
        protected T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B holderId(String holderId) {
            event.holderId = holderId;
            return self();
        }

        public B issuerParticipantContextId(String issuerParticipantContextId) {
            event.issuerParticipantContextId = issuerParticipantContextId;
            return self();
        }

        public B holderProcessId(String holderProcessId) {
            event.holderProcessId = holderProcessId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.issuerParticipantContextId);
            Objects.requireNonNull(event.holderProcessId);
            return event;
        }
    }
}
