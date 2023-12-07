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

package org.eclipse.edc.identithub.did.spi.model;

import java.util.Arrays;

/**
 * The DidState enum represents the state of a DID resource in the internal store.
 */
public enum DidState {
    /**
     * The {@link DidResource} was created in memory, but not yet persisted. This is the default state.
     */
    INITIAL(100),
    /**
     * The {@link DidResource} was created locally in the database, but not yet published.
     */
    GENERATED(200),
    /**
     * The {@link DidResource} has been published to a VDR.
     */
    PUBLISHED(300),
    /**
     * The {@link DidResource} is deleted from the VDR, it is not resolvable anymore.
     */
    UNPUBLISHED(400);

    private final int code;

    DidState(int code) {

        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DidState from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }
}
