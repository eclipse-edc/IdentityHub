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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe DID document changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface DidDocumentListener {

    /**
     * A DID document got published
     */
    default void published(DidDocument document, String participantId) {

    }

    /**
     * A DID document got un-published
     */
    default void unpublished(DidDocument document, String participantId) {

    }
}
