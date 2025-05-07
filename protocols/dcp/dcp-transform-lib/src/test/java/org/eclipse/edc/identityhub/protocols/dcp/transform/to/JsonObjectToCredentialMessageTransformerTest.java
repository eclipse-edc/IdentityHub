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

package org.eclipse.edc.identityhub.protocols.dcp.transform.to;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class JsonObjectToCredentialMessageTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectToCredentialMessageTransformer transformer = new JsonObjectToCredentialMessageTransformer(typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }


    @Test
    void transform() {

        var credentialContainers = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("credentialType", "MembershipCredential")
                        .add("payload", "test-payload")
                        .add("format", "myFormat")
                        .build())
                .build();

        var credentialsJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, credentialContainers));

        var input = Json.createObjectBuilder()
                .add(toIri(ISSUER_PID_TERM), "test-request-id")
                .add(toIri(HOLDER_PID_TERM), "test-holder-id")
                .add(toIri(STATUS_TERM), "ISSUED")
                .add(toIri(CREDENTIALS_TERM), credentialsJsonArray)
                .build();

        var credentialRequestMessage = transformer.transform(input, context);

        assertThat(credentialRequestMessage).isNotNull();
        assertThat(credentialRequestMessage.getCredentials()).hasSize(1).first().satisfies(credentialRequest -> {
            assertThat(credentialRequest.credentialType()).isEqualTo("MembershipCredential");
            assertThat(credentialRequest.format()).isEqualTo("myFormat");
            assertThat(credentialRequest.payload()).isNotNull();
        });
    }

    @Test
    void transform_noCredentials() {

        var credentialsJsonArray = Json.createArrayBuilder().build();

        var input = Json.createObjectBuilder()
                .add(toIri(ISSUER_PID_TERM), "test-request-id")
                .add(toIri(HOLDER_PID_TERM), "test-holder-id")
                .add(toIri(CREDENTIALS_TERM), credentialsJsonArray)
                .add(toIri(STATUS_TERM), "ISSUED")
                .build();

        var credentialRequestMessage = transformer.transform(input, context);

        assertThat(credentialRequestMessage).isNotNull();
        assertThat(credentialRequestMessage.getCredentials()).isEmpty();
    }

    @Test
    void transform_invalidCredentialsJson() {
        var credentialContainers = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("someProperty", "MembershipCredential")
                        .add("whoNeedsThisProperty", "test-payload")
                        .add("alsoNotValid", "myFormat")
                        .build())
                .build();

        var credentialsJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, credentialContainers));

        var input = Json.createObjectBuilder()
                .add(toIri(ISSUER_PID_TERM), "test-request-id")
                .add(toIri(HOLDER_PID_TERM), "test-holder-id")
                .add(toIri(STATUS_TERM), "ISSUED")
                .add(toIri(CREDENTIALS_TERM), credentialsJsonArray)
                .build();

        var credentialRequestMessage = transformer.transform(input, context);

        assertThat(credentialRequestMessage).isNotNull();
        assertThat(credentialRequestMessage.getCredentials()).isEmpty();
        verify(context).reportProblem(contains("Error reading JSON literal"));
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
