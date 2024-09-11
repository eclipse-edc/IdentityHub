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

package org.eclipse.edc.identityhub.spi.keypair.model;

import java.util.Arrays;

/**
 * Possible states a {@link KeyPairResource} can be in.
 */
public enum KeyPairState {
    /**
     * Key pair was created in the database, but is not yet active (e.g. not stored in the DID document)
     */
    CREATED(100),
    /**
     * Key pair is actively used to sign or encrypt material.
     */
    ACTIVATED(200),
    /**
     * The key is not used to sign or encrypt anymore, but it can still be used to verify material that was signed with it in the past. At this point
     * the private key is likely expunged from the vault.
     */
    ROTATED(300),
    /**
     * The key is retired, it cannot be used to either sign/encrypt or verify/decrypt anymore.
     */
    REVOKED(400);

    private final int code;

    KeyPairState(int code) {
        this.code = code;
    }

    public static KeyPairState from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
