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

package org.eclipse.edc.identithub.did.spi;

/**
 * Registry that hosts multiple {@link DidDocumentPublisher}s to dispatch the publishing of a DID document based on
 * its DID method.
 * There can only be one publisher per method.
 */
public interface DidDocumentPublisherRegistry {

    /**
     * Registers a {@link DidDocumentPublisher} for a given DID method.
     *
     * @param didMethodName The DID method name. This may include the "did:" prefix, so both "did:web" and "web" would be valid.
     * @param publisher     The publisher to register
     */
    void addPublisher(String didMethodName, DidDocumentPublisher publisher);

    /**
     * Returns the publisher that was registered for a particular DID method.
     *
     * @param did the DID method for which the publisher was previously registered
     * @return A {@link DidDocumentPublisher}, or null if none was registered.
     */
    DidDocumentPublisher getPublisher(String did);

}
