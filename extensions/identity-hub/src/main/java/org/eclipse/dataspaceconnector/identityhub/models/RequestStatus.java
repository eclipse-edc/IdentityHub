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

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#response-objects">response objects documentation</a>
 * and <a href="https://identity.foundation/decentralized-web-node/spec/#request-level-status-coding">status doc</a>.
 */
public class RequestStatus extends Status {
    public static final RequestStatus OK = new RequestStatus(200, "The request was successfully processed");
    public static final RequestStatus DID_NOT_FOUND = new RequestStatus(404, "Target DID not found within the Decentralized Web Node");
    public static final RequestStatus ERROR = new RequestStatus(500, "The request could not be processed correctly");

    private RequestStatus(int code, String detail) {
        super(code, detail);
    }
}
