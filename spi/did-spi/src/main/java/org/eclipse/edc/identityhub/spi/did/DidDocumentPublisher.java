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

package org.eclipse.edc.identityhub.spi.did;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * The DidDocumentPublisher is responsible for taking a {@link DidDocument} and making it available at a public location.
 * This could be as simple as putting the DID document on a CDN, or more complex implementations could choose to publish
 * the document on a VDR (verifiable data registry),
 */
@ExtensionPoint
public interface DidDocumentPublisher {

    /**
     * Checks if the given ID can be handled by the DidDocumentPublisher. IDs must contain the DID method
     * (see <a href="https://www.w3.org/TR/did-core/#did-syntax">W3C DID 1.0</a>)
     *
     * @param id the ID to be checked. This must conform to the <a href="https://www.w3.org/TR/did-core/#did-syntax">DID syntax</a>
     * @return true if the publisher can handle the ID, false otherwise
     */
    boolean canHandle(String id);

    /**
     * Publishes a given {@link DidDocument} to a verifiable data registry (VDR).
     *
     * @param did the DID of the document to publish
     * @return a {@link Result} object indicating the success or failure of the operation.
     */
    Result<Void> publish(String did);

    /**
     * Unpublishes a given {@link DidDocument} from a verifiable data registry (VDR).
     *
     * @param did the DID of the document to un-publish
     * @return a {@link Result} object indicating the success or failure of the operation.
     */
    Result<Void> unpublish(String did);
}
