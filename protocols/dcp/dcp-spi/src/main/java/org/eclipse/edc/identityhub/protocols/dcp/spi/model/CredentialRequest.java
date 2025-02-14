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

package org.eclipse.edc.identityhub.protocols.dcp.spi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialRequest(@JsonProperty(value = "credentialType", required = true) String credentialType,
                                @JsonProperty(value = "format", required = true) String format,
                                @JsonInclude(JsonInclude.Include.NON_NULL)
                                @JsonProperty("payload") Object payload) {
}
