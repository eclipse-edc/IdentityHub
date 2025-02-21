/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credential.request.model;

import java.util.Arrays;

/**
 * Tracks the state of a holder-initiated credential request ({@link HolderCredentialRequest}).
 */
public enum HolderRequestState {
    /**
     * The {@link HolderCredentialRequest} was created in the database, no interaction with the Issuer has happened yet
     */
    CREATED(100),
    /**
     * The {@link HolderCredentialRequest} has been sent to the Issuer, but no response was received yet
     */
    REQUESTING(200),
    /**
     * The Issuer sent a response to the {@link HolderCredentialRequest}, and the {@link HolderCredentialRequest#getIssuerPid()}
     * contains the Issuer-side ID.
     * Note that failed requests transition to the {@link HolderRequestState#ERROR} state
     */
    REQUESTED(300),
    /**
     * The credential was issued by the Issuer and was stored in the credential storage.
     */
    ISSUED(400),
    /**
     * An error occurred during request processing. Inspect {@link HolderCredentialRequest#getErrorDetail()} for more details
     */
    ERROR(500);

    private final int code;

    HolderRequestState(int code) {
        this.code = code;
    }

    public static HolderRequestState from(int code) {
        return Arrays.stream(values()).filter(ips -> ips.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
