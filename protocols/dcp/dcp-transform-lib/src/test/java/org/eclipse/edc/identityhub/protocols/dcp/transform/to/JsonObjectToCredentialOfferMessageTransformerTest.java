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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToCredentialOfferMessageTransformerTest {

    private final JsonObjectToCredentialOfferMessageTransformer transformer = new JsonObjectToCredentialOfferMessageTransformer(DSPACE_DCP_NAMESPACE_V_1_0);
    private final TransformerContext transformerContext = mock();

    @Test
    void transform() {

        var credentialsObj = CredentialObject.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .credentialType("TestCredential")
                .profile("test-profile")
                .build();
        when(transformerContext.transform(isA(JsonObject.class), eq(CredentialObject.class)))
                .thenReturn(credentialsObj);

        var credentialsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
                        .add(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "offerReason")
                        .add(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                        .add(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("binding")))
                        .build());
        var msg = Json.createObjectBuilder()
                .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(toIri(CREDENTIALS_TERM), credentialsArray)
                .build();
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getIssuer()).isEqualTo("test-issuer");
        assertThat(result.getCredentials()).usingRecursiveFieldByFieldElementComparator().containsExactly(credentialsObj);
    }

    @Test
    void transform_noIssuer() {

        var credentialsObj = CredentialObject.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .credentialType("TestCredential")
                .profile("test-profile")
                .build();
        when(transformerContext.transform(isA(JsonObject.class), eq(CredentialObject.class)))
                .thenReturn(credentialsObj);

        var credentialsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
                        .add(toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "offerReason")
                        .add(toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                        .add(toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("binding")))
                        .build());
        var msg = Json.createObjectBuilder()
                // missing: .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(toIri(CREDENTIALS_TERM), credentialsArray)
                .build();
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getIssuer()).isNull();
    }

    @Test
    void transform_noCredentials() {

        var msg = Json.createObjectBuilder()
                .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .build();
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getIssuer()).isEqualTo("test-issuer");
        assertThat(result.getCredentials()).isEmpty();
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}