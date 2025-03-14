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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.mockito.Mockito.mock;

public class JsonObjectToIssuerMetadataTransformerTest {


    private final JsonObjectToIssuerMetadataTransformer transformer = new JsonObjectToIssuerMetadataTransformer(DSPACE_DCP_NAMESPACE_V_1_0);
    private final TransformerContext context = mock();

    @Test
    void transform() {

        var input = Json.createObjectBuilder()
                .add(IssuerMetadata.ISSUER_METADATA_ISSUER_IRI, "issuer")
                .add(toIri(IssuerMetadata.ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()))
                .build();

        var issuerMetadata = transformer.transform(input, context);

        assertThat(issuerMetadata).isNotNull();
        assertThat(issuerMetadata.getIssuer()).isEqualTo("issuer");
        assertThat(issuerMetadata.getCredentialsSupported()).hasSize(1);
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}


