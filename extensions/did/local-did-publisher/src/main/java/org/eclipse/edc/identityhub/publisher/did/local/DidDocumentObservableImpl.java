package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentListener;
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentObservable;
import org.eclipse.edc.spi.observe.ObservableImpl;

public class DidDocumentObservableImpl extends ObservableImpl<DidDocumentListener> implements DidDocumentObservable {
}
