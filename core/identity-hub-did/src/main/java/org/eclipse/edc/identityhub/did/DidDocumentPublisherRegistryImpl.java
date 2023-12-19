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

package org.eclipse.edc.identityhub.did;

import org.eclipse.edc.identithub.did.spi.DidDocumentPublisher;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisherRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * In-mem variant of the publisher registry.
 */
public class DidDocumentPublisherRegistryImpl implements DidDocumentPublisherRegistry {
    public static final String DID_PREFIX = "did:";
    public static final String DID_METHOD_SEPARATOR = ":";
    private final Map<String, DidDocumentPublisher> publishers = new HashMap<>();

    @Override
    public void addPublisher(String didMethodName, DidDocumentPublisher publisher) {
        publishers.put(didMethodName, publisher);
    }

    @Override
    public DidDocumentPublisher getPublisher(String did) {
        // only need the "did" and method prefix
        did = did.replace(DID_PREFIX, "");
        var endIndex = did.indexOf(DID_METHOD_SEPARATOR);
        return endIndex >= 0 ?
                publishers.get(DID_PREFIX + did.substring(0, endIndex)) :
                null;
    }

}
