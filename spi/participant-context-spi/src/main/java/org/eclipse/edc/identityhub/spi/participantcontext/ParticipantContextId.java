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

package org.eclipse.edc.identityhub.spi.participantcontext;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class ParticipantContextId {

    private static final Pattern BASE64_REGEX = Pattern.compile("^[A-Za-z0-9=_-]*$");

    public static void setMonitor(Monitor monitor) {
        ParticipantContextId.monitor = monitor;
    }

    private static Monitor monitor = new ConsoleMonitor(ConsoleMonitor.Level.WARNING, true);

    private ParticipantContextId() {
    }

    /**
     * Try to decode a base64-url encoded participantContextId. An input string is considered base64 if:
     * <ul>
     *     <li>it consists of characters only of the base64 alphabet ({@link ParticipantContextId#BASE64_REGEX})</li>
     *     <li>its length is a multiple of 4</li>
     *     <li>its length is greater than 4</li>
     *     <li>it can be converted to base64 without an exception</li>
     * </ul>
     *
     * @param encoded possibly base64-url encoded participantContextId
     * @return the decoded participant context id, or just the input string (if not base64)
     */
    public static Result<String> onEncoded(String encoded) {

        if (!BASE64_REGEX.matcher(encoded).matches() ||
                encoded.length() % 4 != 0 ||
                encoded.length() <= 4) {
            return Result.success(encoded);
        }
        try {
            var decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            monitor.warning("A base64-encoded ParticipantContextId was detected: %s (decoded to %s). Base64-encoded participant context IDs will be removed in future releases.".formatted(encoded, decoded));
            return Result.success(decoded);
        } catch (IllegalArgumentException e) {
            return Result.success(encoded);
        }
    }
}
