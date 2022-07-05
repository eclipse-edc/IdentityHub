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

package org.eclipse.dataspaceconnector.identityhub.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#response-objects">response objects documentation</a>
 * and <a href="https://identity.foundation/decentralized-web-node/spec/#message-level-status-coding">status doc</a>.
 */
public class MessageStatus extends Status {
    public static final MessageStatus OK = new MessageStatus(200, "The message was successfully processed");
    public static final MessageStatus MALFORMED_MESSAGE = new MessageStatus(400, "The message was malformed or improperly constructed");
    public static final MessageStatus INTERFACE_NOT_IMPLEMENTED = new MessageStatus(501, "The interface method is not implemented");

    private MessageStatus(@JsonProperty("code") int code, @JsonProperty("detail") String detail) {
        super(code, detail);
    }
}
