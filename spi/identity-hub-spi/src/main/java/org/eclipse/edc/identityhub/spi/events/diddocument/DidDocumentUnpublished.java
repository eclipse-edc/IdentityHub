package org.eclipse.edc.identityhub.spi.events.diddocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class DidDocumentUnpublished extends DidDocumentEvent {
    @Override
    public String name() {
        return null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends DidDocumentEvent.Builder<DidDocumentUnpublished, DidDocumentUnpublished.Builder> {

        private Builder() {
            super(new DidDocumentUnpublished());
        }

        @Override
        public Builder self() {
            return this;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
