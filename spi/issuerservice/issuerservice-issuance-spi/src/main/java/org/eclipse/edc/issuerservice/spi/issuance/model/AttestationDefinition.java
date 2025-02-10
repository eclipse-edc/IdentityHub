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

package org.eclipse.edc.issuerservice.spi.issuance.model;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Defines an attestation that is used to evaluate an issuance request.
 *
 * @param id              the id this definition can be referenced by
 * @param attestationType the type of attestation. For example, the attestation may be a claim in a verifiable
 *                        presentation or an entry in a database table
 * @param configuration   attestation configuration. For example, configuration may include the verified credential type
 *                        required for the attestation and mappings from its claims to output data used to issue a
 *                        credential.
 */
public record AttestationDefinition(String id, String attestationType, Map<String, Object> configuration) {
    public AttestationDefinition {
        requireNonNull(id, "id is required");
        requireNonNull(attestationType, "attestationType is required");
        requireNonNull(configuration, "configuration is required");
    }
}
