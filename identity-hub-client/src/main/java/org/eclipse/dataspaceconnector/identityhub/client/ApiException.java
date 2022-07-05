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

package org.eclipse.dataspaceconnector.identityhub.client;

import okhttp3.Headers;
import okhttp3.ResponseBody;

/**
 * Base exception for IdentityHub rest client, used to represent api errors. 
 */
public class ApiException extends RuntimeException {
    private int code = 0;
    private Headers responseHeaders = null;
    private ResponseBody responseBody = null;

    public ApiException(String message, int code, Headers responseHeaders, ResponseBody responseBody) {
        super(message);
        this.code = code;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public ApiException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }

    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }
}
