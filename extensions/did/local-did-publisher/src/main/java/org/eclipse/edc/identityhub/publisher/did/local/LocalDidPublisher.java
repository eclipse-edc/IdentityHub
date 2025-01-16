/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentObservable;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.identityhub.spi.did.DidConstants.DID_WEB_METHOD_REGEX;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * A DID publisher that maintains "did:web" documents by making them available through a simple HTTP endpoint ({@link DidWebController}).
 * All documents in the database ({@link DidResourceStore})where the {@link DidResource#getState()} == {@link DidState#PUBLISHED}
 * are regarded as published and are made available through an HTTP endpoint.
 */
public class LocalDidPublisher implements DidDocumentPublisher {

    private final DidDocumentObservable observable;
    private final DidResourceStore didResourceStore;
    private final Monitor monitor;

    public LocalDidPublisher(DidDocumentObservable observable, DidResourceStore didResourceStore, Monitor monitor) {
        this.observable = observable;
        this.didResourceStore = didResourceStore;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(String id) {
        return DID_WEB_METHOD_REGEX.matcher(id).matches();
    }

    @Override
    public Result<Void> publish(String did) {
        var existingDocument = didResourceStore.findById(did);
        if (existingDocument == null) {
            return Result.failure("A DID Resource with the ID '%s' was not found.".formatted(did));
        }

        if (isPublished(existingDocument)) {
            monitor.warning("DID '%s' is already published - this action will overwrite it.".formatted(did));
        }

        existingDocument.transitionState(DidState.PUBLISHED);

        return didResourceStore.update(existingDocument)
                .map(v -> success())
                .orElse(f -> failure(f.getFailureDetail()))
                .onSuccess(v -> observable.invokeForEach(l -> l.published(existingDocument.getDocument(), existingDocument.getParticipantContextId())));
    }

    @Override
    public Result<Void> unpublish(String did) {
        var existingDocument = didResourceStore.findById(did);
        if (existingDocument == null) {
            return Result.failure("A DID Resource with the ID '%s' was not found.".formatted(did));
        }

        existingDocument.transitionState(DidState.UNPUBLISHED);
        return didResourceStore.update(existingDocument)
                .map(v -> success())
                .orElse(f -> failure(f.getFailureDetail()))
                .onSuccess(v -> observable.invokeForEach(l -> l.unpublished(existingDocument.getDocument(), existingDocument.getParticipantContextId())));

    }

    private boolean isPublished(DidResource didResource) {
        return didResource.getState() == DidState.PUBLISHED.code();
    }
}
