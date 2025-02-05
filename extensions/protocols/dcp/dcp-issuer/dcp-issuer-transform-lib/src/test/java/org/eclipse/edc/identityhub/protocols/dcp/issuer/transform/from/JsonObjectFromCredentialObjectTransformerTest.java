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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.transform.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILES_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialObject.CREDENTIAL_OBJECT_TERM;
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

        var status = CredentialObject.Builder.newInstance()
                .profiles(List.of("profile1", "profile2"))
                .bindingMethods(List.of("binding1"))
                .credentialType("MembershipCredential")
                .issuancePolicy(PresentationDefinition.Builder.newInstance().id("id").build())
                .offerReason("myReason")
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_OBJECT_TERM));
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_OBJECT_PROFILES_TERM))).contains(Json.createValue("profile1"), Json.createValue("profile2"));
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM))).contains(Json.createValue("binding1"));
        assertThat(jsonLd.getString(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM))).isEqualTo("MembershipCredential");
        assertThat(jsonLd.getString(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM))).isEqualTo("myReason");
        assertThat(jsonLd.getJsonArray(toIri(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM))).first()
                .satisfies(jsonValue -> {
                    assertThat(jsonValue.asJsonObject().getString(TYPE)).isEqualTo(JsonLdKeywords.JSON);
                    assertThat(jsonValue.asJsonObject().getJsonObject(VALUE)).satisfies(issuancePolicy -> {
                        assertThat(issuancePolicy.get("id")).isNotNull().isEqualTo(Json.createValue("id"));
                    });
                });
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
