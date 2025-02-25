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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_STATUS_TERM;
import static org.mockito.Mockito.mock;

public class JsonObjectToCredentialRequestStatusTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToCredentialRequestStatusTransformer transformer = new JsonObjectToCredentialRequestStatusTransformer(DSPACE_DCP_NAMESPACE_V_1_0);

    @Test
    void transform() {

        var input = Json.createObjectBuilder()
                .add(toIri(CREDENTIAL_REQUEST_HOLDER_PID_TERM), "holderPid")
                .add(toIri(CREDENTIAL_REQUEST_ISSUER_PID_TERM), "issuerPid")
                .add(toIri(CREDENTIAL_REQUEST_STATUS_TERM), "ISSUED")
                .build();

        var status = transformer.transform(input, context);

        assertThat(status).isNotNull();
        assertThat(status.getIssuerPid()).isEqualTo("issuerPid");
        assertThat(status.getHolderPid()).isEqualTo("holderPid");
        assertThat(status.getStatus()).isEqualTo(CredentialRequestStatus.Status.ISSUED);
    }

    private String toIri(String term) {
        return DSPACE_DCP_NAMESPACE_V_1_0.toIri(term);
    }
}


