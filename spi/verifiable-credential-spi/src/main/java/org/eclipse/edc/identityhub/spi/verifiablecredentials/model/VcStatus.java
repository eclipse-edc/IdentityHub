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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import java.util.Arrays;

/**
 * Indicates the state of a credential. This tracks the credential from when the IdentityHub requests it to when it expires.
 */
public enum VcStatus {
    INITIAL(100),
    REQUESTING(200),
    REQUESTED(300),
    ISSUING(400),
    ISSUED(500),
    REVOKED(600),
    SUSPENDED(700),
    EXPIRED(800),
    NOT_YET_VALID(900),
    ERROR(-100);

    private final int code;

    VcStatus(int code) {
        this.code = code;
    }

    public static VcStatus from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
