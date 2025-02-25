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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_STATUS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

public class JsonObjectFromCredentialRequestStatusTransformerTest {

    private final TransformerContext context = mock();

    private final JsonObjectFromCredentialRequestStatusTransformer transformer = new JsonObjectFromCredentialRequestStatusTransformer(DSPACE_DCP_NAMESPACE_V_1_0, Json.createBuilderFactory(Map.of()));

    @Test
    void transform() {

        var status = CredentialRequestStatus.Builder.newInstance()
                .issuerPid("issuerPid")
                .holderPid("holderPid")
                .status(CredentialRequestStatus.Status.ISSUED)
                .build();

        var jsonLd = transformer.transform(status, context);

        assertThat(jsonLd).isNotNull();
        assertThat(jsonLd.getString(TYPE)).isEqualTo(toIri(CREDENTIAL_REQUEST_TERM));
        assertThat(jsonLd.getJsonObject(toIri(CREDENTIAL_REQUEST_HOLDER_PID_TERM)).getString(ID)).isEqualTo(status.getHolderPid());
        assertThat(jsonLd.getJsonObject(toIri(CREDENTIAL_REQUEST_ISSUER_PID_TERM)).getString(ID)).isEqualTo(status.getIssuerPid());
        assertThat(jsonLd.getJsonObject(toIri(CREDENTIAL_REQUEST_STATUS_TERM)).getString(ID)).isEqualTo(status.getStatus().name());
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}
