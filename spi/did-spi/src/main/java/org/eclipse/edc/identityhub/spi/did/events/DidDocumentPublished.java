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
 * Event that signals that a DID document was published.
 */
@JsonDeserialize(builder = DidDocumentPublished.Builder.class)
public class DidDocumentPublished extends DidDocumentEvent {
    @Override
    public String name() {
        return "diddocument.published";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends DidDocumentEvent.Builder<DidDocumentPublished, Builder> {

        private Builder() {
            super(new DidDocumentPublished());
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
