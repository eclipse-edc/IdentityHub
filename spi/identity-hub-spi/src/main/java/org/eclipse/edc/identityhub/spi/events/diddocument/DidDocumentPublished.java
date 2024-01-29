package org.eclipse.edc.identityhub.spi.events.diddocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class DidDocumentPublished extends DidDocumentEvent {
    @Override
    public String name() {
        return null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends DidDocumentEvent.Builder<DidDocumentPublished, DidDocumentPublished.Builder> {

        private Builder() {
            super(new DidDocumentPublished());
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
