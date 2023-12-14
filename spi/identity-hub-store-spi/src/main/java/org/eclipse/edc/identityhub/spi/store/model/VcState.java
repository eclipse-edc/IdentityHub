/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.store.model;

import java.util.Arrays;

public enum VcState {
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

    VcState(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static VcState from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }
}
