/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.models;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes status of the request done by calling the identity-hub endpoint.
 */
abstract class Status {
    private final int code;
    private final String detail;

    Status(int code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    @Schema(description = "An integer set to the HTTP Status Code appropriate for the status of the response")
    public int getCode() {
        return code;
    }

    @Schema(description = "A string that describes a terse summary of the status")
    public String getDetail() {
        return detail;
    }
}
