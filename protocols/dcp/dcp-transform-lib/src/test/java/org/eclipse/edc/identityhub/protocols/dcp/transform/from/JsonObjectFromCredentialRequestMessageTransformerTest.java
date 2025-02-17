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

package org.eclipse.edc.identityhub.protocols.dcp.transform.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequest;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromCredentialRequestMessageTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectFromCredentialRequestMessageTransformer transformer = new JsonObjectFromCredentialRequestMessageTransformer(typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void transform() {

        when(context.transform(isA(CredentialRequest.class), eq(JsonObject.class))).thenReturn(JsonObject.EMPTY_JSON_OBJECT);
        var status = CredentialRequestMessage.Builder.newInstance()
                .credential(new CredentialRequest("test-request-id", "MembershipCredential", "myFormat", null))
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_REQUEST_MESSAGE_TERM));
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM))).hasSize(1)
                .first().satisfies(jsonValue -> {
                    var credentials = jsonValue.asJsonObject().getJsonArray(JsonLdKeywords.VALUE);
                    assertThat(credentials).hasSize(1);
                    var credential = credentials.getJsonObject(0);
                    assertThat(credential.getString("credentialType")).isEqualTo("MembershipCredential");
                    assertThat(credential.getString("format")).isEqualTo("myFormat");
                    assertThat(credential.get("payload")).isNull();
                });
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
