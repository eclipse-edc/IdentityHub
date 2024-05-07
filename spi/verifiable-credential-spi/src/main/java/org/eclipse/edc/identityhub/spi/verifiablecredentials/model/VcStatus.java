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

import java.util.List;

public enum VcStatus {
    VALID(0),
    EXPIRED(100),
    NOT_YET_VALID(200),
    SUSPENDED(300),
    REVOKED(400),
    OTHER(500);

    public static final List<VcStatus> KNOWN_STATUS_VALUES = List.of(
            VALID, EXPIRED, NOT_YET_VALID, SUSPENDED, REVOKED, OTHER
    );
    private final int code;

    VcStatus(int code) {
        this.code = code;
    }

    public static VcStatus from(int code) {
        return KNOWN_STATUS_VALUES.stream().filter(tps -> tps.code() == code).findFirst().orElse(OTHER);
    }


    public int code() {
        return code;
    }
}
