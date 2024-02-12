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

package org.eclipse.edc.identityhub.participantcontext;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates an API token that encodes the user ID by taking a random byte array of configurable length (default=64), base64-encoding it and the appending it to the base64-encoded user ID
 * separated by a dot ".": {@code base64(userid).base64(rnd-bytes)}
 */
public class ApiTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();
    private final int bound;

    /**
     * Instantiates this generator with the given bound.
     *
     * @param bound the length of the byte array that is filled with random data
     */
    public ApiTokenGenerator(int bound) {
        this.bound = bound;
    }

    /**
     * Instantiates this generator with a default bound of 64.
     */
    public ApiTokenGenerator() {
        this.bound = 64;
    }

    public String generate(String principal) {
        byte[] array = new byte[bound];
        secureRandom.nextBytes(array);
        var enc = Base64.getEncoder();
        return enc.encodeToString(principal.getBytes()) + "." + enc.encodeToString(array);

    }
}
