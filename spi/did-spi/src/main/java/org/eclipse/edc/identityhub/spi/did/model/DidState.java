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

package org.eclipse.edc.identityhub.spi.did.model;

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

    public static DidState from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
