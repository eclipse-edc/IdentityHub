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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution;

import org.eclipse.edc.spi.result.Failure;

import java.util.List;

public class QueryFailure extends Failure {
    private final Reason reason;

    QueryFailure(List<String> messages, Reason reason) {
        super(messages);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVALID_SCOPE,
        STORAGE_FAILURE,
        UNAUTHORIZED_SCOPE,
        OTHER
    }
}
