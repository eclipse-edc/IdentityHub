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

import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_ISSUER_IRI;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromIssuerMetadataTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromIssuerMetadataTransformer transformer = new JsonObjectFromIssuerMetadataTransformer(DSPACE_DCP_NAMESPACE_V_1_0);

    @Test
    void transform() {

        when(context.transform(isA(CredentialObject.class), eq(JsonObject.class))).thenReturn(JsonObject.EMPTY_JSON_OBJECT);

        var issuerMetadata = IssuerMetadata.Builder.newInstance()
                .issuer("issuer")
                .credentialSupported(CredentialObject.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .build();

        var jsonLd = transformer.transform(issuerMetadata, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(ISSUER_METADATA_TERM));
        assertThat(jsonLd.getJsonObject(ISSUER_METADATA_ISSUER_IRI).getString(ID)).isEqualTo("issuer");
        assertThat(jsonLd.getJsonArray(toIri(ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM))).hasSize(1);

    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
