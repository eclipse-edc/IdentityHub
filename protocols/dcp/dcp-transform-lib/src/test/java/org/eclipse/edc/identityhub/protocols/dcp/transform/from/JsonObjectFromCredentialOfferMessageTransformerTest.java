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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JsonObjectFromCredentialOfferMessageTransformerTest {

    private final JsonObjectFromCredentialOfferMessageTransformer transformer = new JsonObjectFromCredentialOfferMessageTransformer(DSPACE_DCP_NAMESPACE_V_1_0);
    private final TransformerContext transformerContext = mock();

    @Test
    void transform() {

        var msg = CredentialOfferMessage.Builder.newInstance()
                .issuer("test-issuer")
                .credentials(List.of(
                        CredentialObject.Builder.newInstance()
                                .id(UUID.randomUUID().toString())
                                .credentialType("TestCredential")
                                .profile("test-profile")
                                .offerReason("reissuance")
                                .bindingMethod("did:web")
                                .issuancePolicy(null)
                                .build()
                )).build();
        when(transformerContext.transform(isA(CredentialObject.class), eq(JsonObject.class)))
                .thenReturn(Json.createObjectBuilder().build());
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getString(toIri("issuer"))).isEqualTo("test-issuer");
        assertThat(result.getJsonArray(toIri("credentials"))).hasSize(1);

        verify(transformerContext).transform(isA(CredentialObject.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerContext);
    }

    @Test
    void transform_sparse() {

        var msg = CredentialOfferMessage.Builder.newInstance()
                .issuer("test-issuer")
                .credentials(List.of(
                        CredentialObject.Builder.newInstance()
                                .id(UUID.randomUUID().toString())
                                .build()
                )).build();
        when(transformerContext.transform(isA(CredentialObject.class), eq(JsonObject.class)))
                .thenReturn(Json.createObjectBuilder().build());
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getString(toIri("issuer"))).isEqualTo("test-issuer");
        assertThat(result.getJsonArray(toIri("credentials"))).hasSize(1);

        verify(transformerContext).transform(isA(CredentialObject.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerContext);
    }

    @Test
    void transform_noCredentials() {

        var msg = CredentialOfferMessage.Builder.newInstance()
                .issuer("test-issuer")
                .build();
        var result = transformer.transform(msg, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.getString(toIri("issuer"))).isEqualTo("test-issuer");
        assertThat(result.getJsonArray(toIri("credentials"))).isEmpty();

        verifyNoMoreInteractions(transformerContext);
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}