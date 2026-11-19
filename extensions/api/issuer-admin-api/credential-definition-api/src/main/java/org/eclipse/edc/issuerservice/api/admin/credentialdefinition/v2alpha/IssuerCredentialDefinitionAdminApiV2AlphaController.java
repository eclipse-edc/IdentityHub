/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v2alpha;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile;
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.IssuerCredentialDefinitionAdminApiController;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Serves all endpoints of the v1 {@link IssuerCredentialDefinitionAdminApiController} under {@code /v2alpha}.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/credentialdefinitions")
public class IssuerCredentialDefinitionAdminApiV2AlphaController extends IssuerCredentialDefinitionAdminApiController implements IssuerCredentialDefinitionAdminApiV2Alpha {

    public IssuerCredentialDefinitionAdminApiV2AlphaController(AuthorizationService authorizationService, CredentialDefinitionService credentialDefinitionService,
                                                               DcpIssuerProfile issuerProfile) {
        super(authorizationService, credentialDefinitionService, issuerProfile);
    }

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }
}
