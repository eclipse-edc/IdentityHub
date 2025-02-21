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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a credential request
 *
 * @param issuerDid   The DID of the issuer.
 * @param holderPid   A client-assigned request ID. A random ID will be assigned if null
 * @param credentials A list of credential descriptors
 */
public record CredentialRequestDto(@JsonProperty(required = true) String issuerDid,
                                   @JsonProperty @Nullable String holderPid,
                                   @JsonProperty(required = true) List<CredentialDescriptor> credentials) {
}
