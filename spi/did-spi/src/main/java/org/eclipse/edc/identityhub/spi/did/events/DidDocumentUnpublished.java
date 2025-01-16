/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.spi.did.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Event that signals that a DID document was un-published.
 */
@JsonDeserialize(builder = DidDocumentUnpublished.Builder.class)
public class DidDocumentUnpublished extends DidDocumentEvent {
    @Override
    public String name() {
        return "diddocument.unpublished";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends DidDocumentEvent.Builder<DidDocumentUnpublished, Builder> {

        private Builder() {
            super(new DidDocumentUnpublished());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
