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

import org.eclipse.edc.spi.result.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ParticipantContextId {

    private ParticipantContextId() {
    }

    /**
     * Decode a base64-url encoded participantId.
     *
     * @param encoded base64-url encoded participantContextId.
     * @return human-readable participantContextId.
     */
    public static Result<String> onEncoded(String encoded) {
        var bytes = Base64.getUrlDecoder().decode(encoded.getBytes());
        return Result.success(new String(bytes, StandardCharsets.UTF_8));
    }
}
