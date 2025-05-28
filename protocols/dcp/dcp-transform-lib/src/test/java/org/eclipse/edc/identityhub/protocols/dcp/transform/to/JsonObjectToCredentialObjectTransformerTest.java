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
import org.eclipse.edc.transform.TransformerContextImpl;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectToCredentialObjectTransformerTest {

    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonObjectToCredentialObjectTransformer transformer = new JsonObjectToCredentialObjectTransformer(typeManager, "test", DSPACE_DCP_NAMESPACE_V_1_0);
    private final TypeTransformerRegistry trr = new TypeTransformerRegistryImpl();
    private final TransformerContext context = new TransformerContextImpl(trr);

    @BeforeEach
    void setUp() {
        trr.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }


    @Test
    void transform() {

        var issuancePolicy = Json.createObjectBuilder()
                .add("id", "id")
                .build();

        var issuancePolicyJsonLd = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, issuancePolicy));

        var input = Json.createObjectBuilder()
                .add(JsonLdKeywords.ID, UUID.randomUUID().toString())
                .add(toIri(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM), issuancePolicyJsonLd)
                .add(toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
                .add(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "offerReason")
                .add(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                .add(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("binding")))
                .build();

        var credentialObject = transformer.transform(input, context);

        assertThat(credentialObject).isNotNull();
        assertThat(credentialObject.getId()).isNotNull();
        assertThat(credentialObject.getOfferReason()).isEqualTo("offerReason");
        assertThat(credentialObject.getCredentialType()).isEqualTo("MembershipCredential");
        assertThat(credentialObject.getProfile()).isEqualTo("profile");
        assertThat(credentialObject.getBindingMethods()).hasSize(1).contains("binding");
        assertThat(credentialObject.getIssuancePolicy()).isNotNull()
                .satisfies(presentationDefinition -> {
                    assertThat(presentationDefinition.getId()).isEqualTo("id");
                });
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}


