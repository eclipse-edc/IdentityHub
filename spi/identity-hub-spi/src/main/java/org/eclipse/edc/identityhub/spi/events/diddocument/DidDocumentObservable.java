package org.eclipse.edc.identityhub.spi.events.diddocument;

import org.eclipse.edc.spi.observe.Observable;

/**
 * Manages and invokes {@link DidDocumentListener}s when a state change related to a DID document resource has happened.
 */
public interface DidDocumentObservable extends Observable<DidDocumentListener> {
}
