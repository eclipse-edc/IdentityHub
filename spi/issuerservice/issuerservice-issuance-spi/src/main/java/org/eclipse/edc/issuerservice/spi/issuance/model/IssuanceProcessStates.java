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

package org.eclipse.edc.issuerservice.spi.issuance.model;

import java.util.Arrays;

public enum IssuanceProcessStates {
    SUBMITTED(50),
    APPROVED(100),
    DELIVERED(200),
    ERRORED(300);

    private final int code;

    IssuanceProcessStates(int code) {
        this.code = code;
    }

    public static IssuanceProcessStates from(int code) {
        return Arrays.stream(values()).filter(ips -> ips.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
