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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class CredentialRequestMessageValidatorTest {
    private final Validator<JsonObject> validator = CredentialRequestMessageValidator.instance(DSPACE_DCP_NAMESPACE_V_1_0);


    @Test
    void validate_success() {
        var jo = createObjectBuilder()
                .add(JsonLdKeywords.TYPE, createArrayBuilder().add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)))
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM), createArrayBuilder()
                        .add(createObjectBuilder()))
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM), createArrayBuilder()
                        .add(createObjectBuilder().add(JsonLdKeywords.ID, UUID.randomUUID().toString())))
                .build();

        assertThat(validator.validate(jo)).isSucceeded();
    }

    @Test
    void validate_missingFields() {
        var jo = createObjectBuilder()
                .build();

        assertThat(validator.validate(jo)).isFailed();
    }
}
