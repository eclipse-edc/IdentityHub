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

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class CredentialMessageValidatorTest {

    private final CredentialMessageValidator validator = new CredentialMessageValidator();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());

    @Test
    void validate_success() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(ISSUER_PID_TERM), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(HOLDER_PID_TERM), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentialType"), "SomeCredential")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("format"), "vcdm11_jwt")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("payload"), "SOME_JWT_STRING")))
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isSucceeded();
    }

    @Test
    void validate_emptyCredentials_success() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(ISSUER_PID_TERM), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(HOLDER_PID_TERM), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder())
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isSucceeded();
    }

    @Test
    void validate_requestIdMissing_failure() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                // missing: requestId
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentialType"), "SomeCredential")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("format"), "vcdm11_jwt")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("payload"), "SOME_JWT_STRING")))
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isFailed();
    }

    @Test
    void validate_requestIdNull_failure() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("requestId"), JsonValue.NULL)
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentials"), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentialType"), "SomeCredential")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("format"), "vcdm11_jwt")
                                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("payload"), "SOME_JWT_STRING")))
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isFailed();
    }

    @Test
    void validate_credentialsArrayNull_failure() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("requestId"), UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("credentials"), JsonValue.NULL)
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isFailed();
    }

    @Test
    void validate_credentialsArrayMissing_failure() {
        var msg = Json.createObjectBuilder()
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri("requestId"), UUID.randomUUID().toString())
                // missing: credentials array
                .build();
        assertThat(validator.validate(jsonLd.expand(msg).getContent())).isFailed();
    }
}
