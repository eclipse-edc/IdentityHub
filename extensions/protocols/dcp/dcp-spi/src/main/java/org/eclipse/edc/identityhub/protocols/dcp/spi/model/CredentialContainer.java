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

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialContainer(@JsonProperty(required = true) String credentialType,
                                  @JsonProperty(required = true) String format,
                                  @JsonProperty(required = true) String payload) {

}
