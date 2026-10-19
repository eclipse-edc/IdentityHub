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
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a URL into a did:web identifier by parsing the authority and the path. A DID document is served under exactly three
 * URLs, all other URLs are rejected:
 * <pre>
 *     https://foo.bar/some/path                      -> did:web:foo.bar:some:path
 *     https://foo.bar/some/path/did.json             -> did:web:foo.bar:some:path
 *     https://foo.bar/some/path/.well-known/did.json -> did:web:foo.bar:some:path
 *     https://foo.bar/                               -> did:web:foo.bar
 *     https://foo.bar/did.json                       -> did:web:foo.bar
 *     https://foo.bar/.well-known/did.json           -> did:web:foo.bar
 * </pre>
 */
@ExtensionPoint
public class DidWebParser {

    private static final String SEPARATOR = "/";
    private static final String WELL_KNOWN_SEGMENT = DidConstants.WELL_KNOWN.substring(1);

    /**
     * Parses a HTTP URL using the specified charset by performing the following steps:
     * <ul>
     *     <li>strip away an exact trailing "did.json" path segment, if present</li>
     *     <li>if "did.json" was stripped, strip away an exact trailing ".well-known" path segment, if present</li>
     *     <li>replace all remaining slashes with colons ":" in the path</li>
     *     <li>URL-Encode the authority ("host:port") to clearly distinguish it from the method separator ":"</li>
     *     <li>prepend "did:web:"</li>
     * </ul>
     * Every other URL, for example one with empty path segments, a trailing slash, or a remaining "did.json" or ".well-known"
     * segment, does not map to a DID, so that every DID document is reachable only under the URLs listed above.
     *
     * @param url     The input URL
     * @param charset The charset used for encoding the {@link URL#getAuthority()}
     * @return a "did:web:XYZ" identifier, or null if the URL does not map to a DID
     */
    public @Nullable String parse(URI url, Charset charset) {
        var segments = pathSegments(url.getPath());
        if (segments == null) {
            return null;
        }

        if (removeLast(segments, DidConstants.DID_WEB_DID_DOCUMENT)) {
            removeLast(segments, WELL_KNOWN_SEGMENT);
        }
        if (segments.contains(DidConstants.DID_WEB_DID_DOCUMENT) || segments.contains(WELL_KNOWN_SEGMENT)) {
            return null;
        }

        // ports must be percent-encoded:
        var identifier = new StringBuilder(URLEncoder.encode(url.getAuthority(), charset));
        segments.forEach(segment -> identifier.append(":").append(segment));

        return "%s:%s".formatted(DidConstants.DID_WEB_METHOD, identifier);
    }

    /**
     * Splits the path into its segments. Returns null if the path contains empty segments, i.e. double or trailing slashes.
     * The root path "/" and the empty path have no segments.
     */
    private static @Nullable List<String> pathSegments(String path) {
        if (path == null || path.isEmpty() || path.equals(SEPARATOR)) {
            return new ArrayList<>();
        }
        var segments = new ArrayList<>(Arrays.asList(path.substring(1).split(SEPARATOR, -1)));
        return segments.contains("") ? null : segments;
    }

    private static boolean removeLast(List<String> segments, String segment) {
        if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(segment)) {
            segments.remove(segments.size() - 1);
            return true;
        }
        return false;
    }

}
