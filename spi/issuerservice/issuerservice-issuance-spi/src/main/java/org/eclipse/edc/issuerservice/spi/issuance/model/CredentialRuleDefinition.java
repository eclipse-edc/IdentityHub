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
 * A rule that is executed during credential issuance.
 *
 * @param type the rule type
 * @param configuration rule configuration
 */
public record CredentialRuleDefinition(String type, Map<String, Object> configuration) {
    public CredentialRuleDefinition {
        requireNonNull(type, "type is required");
        requireNonNull(configuration, "configuration is required");
    }
}
