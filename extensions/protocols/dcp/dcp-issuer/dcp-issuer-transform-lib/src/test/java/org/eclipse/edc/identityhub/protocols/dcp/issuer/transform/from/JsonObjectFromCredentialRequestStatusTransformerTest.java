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

import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_REQUEST_ID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_STATUS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

public class JsonObjectFromCredentialRequestStatusTransformerTest {

    private final TransformerContext context = mock();

    private final JsonObjectFromCredentialRequestStatusTransformer transformer = new JsonObjectFromCredentialRequestStatusTransformer(DSPACE_DCP_NAMESPACE_V_1_0);

    @Test
    void transform() {

        var status = CredentialRequestStatus.Builder.newInstance()
                .requestId("requestId")
                .status(CredentialRequestStatus.Status.ISSUED)
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_REQUEST_TERM));
        assertThat(jsonLd.getString(toIri(CREDENTIAL_REQUEST_REQUEST_ID_TERM))).isEqualTo(status.getRequestId());
        assertThat(jsonLd.getString(toIri(CREDENTIAL_REQUEST_STATUS_TERM))).isEqualTo(status.getStatus().name());
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
