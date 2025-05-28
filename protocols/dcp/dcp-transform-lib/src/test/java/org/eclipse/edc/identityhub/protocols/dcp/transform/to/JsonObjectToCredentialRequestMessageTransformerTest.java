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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonObjectToCredentialRequestMessageTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectToCredentialRequestMessageTransformer transformer = new JsonObjectToCredentialRequestMessageTransformer(typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }


    @Test
    void transform() {

        var credentialRequests = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("id", "membershipCredential-object-id")
                        .add("format", "myFormat")
                        .build())
                .build();

        var credentialsJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, credentialRequests));

        var input = Json.createObjectBuilder()
                .add(toIri(CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM), UUID.randomUUID().toString())

                .add(toIri(CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM), credentialsJsonArray)
                .build();

        var credentialRequestMessage = transformer.transform(input, context);

        assertThat(credentialRequestMessage).isNotNull();
        assertThat(credentialRequestMessage.getHolderPid()).isNotNull();
        assertThat(credentialRequestMessage.getCredentials()).hasSize(1).first().satisfies(credentialRequest -> {
            assertThat(credentialRequest.credentialObjectId()).isEqualTo("membershipCredential-object-id");
        });
    }

    @Test
    void transform_withWrongCredentialRequest() {


        var credentialRequests = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .build())
                .build();

        var credentialsJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, credentialRequests));

        var input = Json.createObjectBuilder()
                .add(toIri(CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM), credentialsJsonArray)
                .build();

        var credentialRequestMessage = transformer.transform(input, context);

        assertThat(credentialRequestMessage).isNotNull();
        assertThat(credentialRequestMessage.getCredentials()).isEmpty();

        verify(context).reportProblem(any());
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}


