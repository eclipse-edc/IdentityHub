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

import static java.util.Objects.requireNonNull;

/**
 * Maps a claim value in the issuance context to a property on the generated credential.
 *
 * @param input    the claim value. Nested properties are supported using the '.' delimiter. For example, <code>foo.bar</code>
 *                 will return the value of the <code>bar</code> property on the <code>foo</code> object.
 * @param output   the property to set on the generated credential. For example, <code>credentialSubject.name</code> will set the
 *                 <code>name</code> property on the credential's <code>credentialSubject</code>.
 * @param required if false and the input claim is not found, the mapping should be ignored. If true and the input claim is not
 *                 found, an error should be raised.
 */
public record MappingDefinition(String input, String output, boolean required) {
    public MappingDefinition {
        requireNonNull(input, "input is required");
        requireNonNull(output, "output is required");
    }
}
