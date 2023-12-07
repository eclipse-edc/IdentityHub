/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.did.spi;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * The DidDocumentPublisher is responsible for taking a {@link DidDocument} and making it available at a VDR (verifiable data registry).
 * For example, an implementation may choose to publish the DID to a CDN.
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
     * @param document the {@link DidDocument} to be published
     * @return a {@link Result} object indicating the success or failure of the operation.
     */
    Result<Void> publish(DidDocument document);

    /**
     * Unpublishes a given {@link DidDocument} from a verifiable data registry (VDR).
     *
     * @param document the {@link DidDocument} to be unpublished
     * @return a {@link Result} object indicating the success or failure of the operation.
     */
    Result<Void> unpublish(DidDocument document);
}
