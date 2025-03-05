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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public record AttestationDefinitionRequest(String id, String attestationType, Map<String, Object> configuration) {
    public AttestationDefinitionRequest {
        requireNonNull(id, "id is required");
        requireNonNull(attestationType, "attestationType is required");
        requireNonNull(configuration, "configuration is required");
    }
}