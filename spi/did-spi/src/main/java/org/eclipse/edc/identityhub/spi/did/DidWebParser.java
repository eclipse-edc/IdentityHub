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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Converts a URL into a did:web identifier by parsing the authority and the path. For example the following conversion applies:
 * <pre>
 *     https://foo.bar/some/path/.well-known/did.json -> did:web:foo.bar:some:path
 *     https://foo.bar/some/path -> did:web:foo.bar:some:path
 * </pre>
 */
@ExtensionPoint
public class DidWebParser {

    /**
     * Parses a HTTP URL using the specified charset by performing the following steps:
     * <ul>
     *     <li>strip away any trailing slash from the path</li>
     *     <li>strip away "did.json" and ".well-known" if present on the path</li>
     *     <li>replace all remaining slashes with colons ":" in the path</li>
     *     <li>URL-Encode the authority ("host:port") to clearly distinguish it from the method separator ":"</li>
     *     <li>prepend "did:web:"</li>
     * </ul>
     *
     * @param url     The input URL
     * @param charset The charset used for encoding the {@link URL#getAuthority()}. Defaults to {@link Charset#defaultCharset()}
     * @return a "did:web:XYZ" identifier
     */
    public String parse(URI url, Charset charset) {
        var path = url.getPath();
        path = stripTrailingSlash(path);

        if (path.endsWith(DidConstants.DID_WEB_DID_DOCUMENT)) {
            path = path.substring(0, path.indexOf(DidConstants.DID_WEB_DID_DOCUMENT));
            path = stripTrailingSlash(path);
        }
        if (path.endsWith(DidConstants.WELL_KNOWN)) {
            path = path.replace(DidConstants.WELL_KNOWN, "");
            path = stripTrailingSlash(path);
        }
        path = path.replace("/", ":");

        // ports must be percent-encoded:
        var identifier = "%s%s".formatted(URLEncoder.encode(url.getAuthority(), charset), path);

        return "%s:%s".formatted(DidConstants.DID_WEB_METHOD, identifier);
    }

    @NotNull
    private String stripTrailingSlash(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        return path;
    }

}
