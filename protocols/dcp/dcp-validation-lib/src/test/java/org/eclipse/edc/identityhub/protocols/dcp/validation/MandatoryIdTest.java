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

package org.eclipse.edc.identityhub.protocols.dcp.validation;

import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class MandatoryIdTest {
    
    @Test
    void validateMandatoryId_success() {
        var input = createObjectBuilder().add("mandatoryId", createArrayBuilder().add(createObjectBuilder()
                .add(JsonLdKeywords.ID, "someId")));

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryId", MandatoryId::new)
                .build()
                .validate(input.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void validateMandatoryId_failure_whenPropertyMissing() {
        var input = createObjectBuilder();

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryId", MandatoryId::new)
                .build()
                .validate(input.build());

        assertThat(result).isFailed();
    }

    @Test
    void validateMandatoryId_failure_whenIdMissing() {
        var input = createObjectBuilder().add("mandatoryId", createArrayBuilder().add(createObjectBuilder()));
        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryId", MandatoryId::new)
                .build()
                .validate(input.build());

        assertThat(result).isFailed();
    }
}
