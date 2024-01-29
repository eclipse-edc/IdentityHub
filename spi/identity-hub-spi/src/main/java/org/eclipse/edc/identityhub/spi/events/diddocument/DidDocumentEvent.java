package org.eclipse.edc.identityhub.spi.events.diddocument;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

public abstract class DidDocumentEvent extends Event {
    protected String did;
    protected String participantId;

    public String getDid() {
        return did;
    }

    public String getParticipantId() {
        return participantId;
    }

    public abstract static class Builder<T extends DidDocumentEvent, B extends DidDocumentEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B participantId(String assetId) {
            event.participantId = assetId;
            return self();
        }

        public B did(String did) {
            event.did = did;
            return self();
        }

        public T build() {
            Objects.requireNonNull((event.participantId));
            return event;
        }
    }
}
