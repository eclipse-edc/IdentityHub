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
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromCredentialObjectTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectFromCredentialObjectTransformer transformer = new JsonObjectFromCredentialObjectTransformer(typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void transform() {

        var credentialObject = CredentialObject.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .profile("profile1")
                .bindingMethods(List.of("binding1"))
                .credentialType("MembershipCredential")
                .issuancePolicy(PresentationDefinition.Builder.newInstance().id("id").build())
                .offerReason("myReason")
                .build();

        var jsonLd = transformer.transform(credentialObject, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_OBJECT_TERM));
        assertThat(jsonLd.getJsonObject(toIri(CREDENTIAL_OBJECT_PROFILE_TERM)).getString("@value")).isEqualTo("profile1");
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM))).contains(stringValue("binding1"));
        assertThat(jsonLd.getString(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM))).isEqualTo("MembershipCredential");
        assertThat(jsonLd.getJsonObject(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM))).isEqualTo(stringValue("myReason"));
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM))).first()
                .satisfies(jsonValue -> {
                    assertThat(jsonValue.asJsonObject().getString(TYPE)).isEqualTo(JsonLdKeywords.JSON);
                    assertThat(jsonValue.asJsonObject().getJsonObject(VALUE)).satisfies(issuancePolicy -> {
                        assertThat(issuancePolicy.get("id")).isNotNull().isEqualTo(Json.createValue("id"));
                    });
                });
    }

    private JsonObject stringValue(String value) {
        return Json.createObjectBuilder().add(VALUE, value).add(TYPE, "http://www.w3.org/2001/XMLSchema#string").build();

    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
