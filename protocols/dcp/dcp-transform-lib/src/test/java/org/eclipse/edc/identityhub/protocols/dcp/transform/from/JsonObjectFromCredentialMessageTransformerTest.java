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
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialContainer;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.Builder;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromCredentialMessageTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());

    private final JsonObjectFromCredentialMessageTransformer transformer = new JsonObjectFromCredentialMessageTransformer(factory, typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void transform() {

        when(context.transform(isA(CredentialMessage.class), eq(JsonObject.class))).thenReturn(JsonObject.EMPTY_JSON_OBJECT);
        var status = Builder.newInstance()
                .issuerPid("issuerId")
                .holderPid("holderId")
                .status("ISSUED")
                .credential(new CredentialContainer("MembershipCredential", "myFormat", "SOMEPAYLOAD"))
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_MESSAGE_TERM));
        assertThat(jsonLd.getJsonObject(toIri(ISSUER_PID_TERM)).getString(ID)).isEqualTo("issuerId");
        assertThat(jsonLd.getJsonObject(toIri(HOLDER_PID_TERM)).getString(ID)).isEqualTo("holderId");

        assertThat(jsonLd.getJsonArray(toIri(CREDENTIALS_TERM))).hasSize(1)
                .first().satisfies(jsonValue -> {
                    var credentials = jsonValue.asJsonObject().getJsonArray(JsonLdKeywords.VALUE);
                    assertThat(credentials).hasSize(1);
                    var credential = credentials.getJsonObject(0);
                    assertThat(credential.getString("credentialType")).isEqualTo("MembershipCredential");
                    assertThat(credential.getString("format")).isEqualTo("myFormat");
                    assertThat(credential.get("payload")).isNotNull();
                });
    }

    @Test
    void transform_noCredentials() {

        when(context.transform(isA(CredentialMessage.class), eq(JsonObject.class))).thenReturn(JsonObject.EMPTY_JSON_OBJECT);
        var status = Builder.newInstance()
                .issuerPid("requestId")
                .holderPid("holderId")
                .status("ISSUED")
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_MESSAGE_TERM));
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIALS_TERM))).hasSize(1)
                .first().satisfies(jsonValue -> {
                    var credentials = jsonValue.asJsonObject().getJsonArray(JsonLdKeywords.VALUE);
                    assertThat(credentials).isEmpty();
                });
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
