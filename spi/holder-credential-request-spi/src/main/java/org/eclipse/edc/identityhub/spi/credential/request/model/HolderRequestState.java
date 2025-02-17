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

public enum HolderRequestState {
    CREATED(100),
    REQUESTING(200),
    REQUESTED(300),
    ISSUED(400),
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
