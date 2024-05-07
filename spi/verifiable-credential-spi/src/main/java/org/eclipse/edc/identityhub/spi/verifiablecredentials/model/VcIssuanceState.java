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
 * Indicates the state of issuance of a credential. This is orthogonal to the status of a credential, which could be "valid", "expired, "suspended", "revoked", etc.
 */
public enum VcIssuanceState {
    INITIAL(100),
    REQUESTING(200),
    REQUESTED(300),
    ISSUING(400),
    ISSUED(500),
    REISSUE_REQUESTING(600),
    REISSUE_REQUESTED(700),
    TERMINATED(800),
    ERROR(900);

    private final int code;

    VcIssuanceState(int code) {
        this.code = code;
    }

    public static VcIssuanceState from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
