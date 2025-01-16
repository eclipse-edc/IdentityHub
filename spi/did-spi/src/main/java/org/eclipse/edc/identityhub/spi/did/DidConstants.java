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

import java.util.regex.Pattern;

public interface DidConstants {
    /**
     * Constant for the DID:WEB method
     */
    String DID_WEB_METHOD = "did:web";
    /**
     * Pattern for use to parse a DID:WEB identifier
     */
    Pattern DID_WEB_METHOD_REGEX = Pattern.compile("(?i)did:web:.+");
    /**
     * the /.well-known path extension. Useful when resolving or parsing DIDs. Must be ignored when parsing a URL to a DID.
     */
    String WELL_KNOWN = "/.well-known";
    /**
     * last path segment when resolving DID documents. Must be clipped off before parsing a URL to a DID
     */
    String DID_WEB_DID_DOCUMENT = "did.json";
}
