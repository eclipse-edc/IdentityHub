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

package org.eclipse.edc.issuerservice.api.admin.credentials.v2alpha;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.IssuerAttestationAdminApiController;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Serves all endpoints of the v1 {@link IssuerAttestationAdminApiController} under {@code /v2alpha}.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/attestations")
public class IssuerAttestationAdminApiV2AlphaController extends IssuerAttestationAdminApiController implements IssuerAttestationAdminApiV2Alpha {

    public IssuerAttestationAdminApiV2AlphaController(AuthorizationService authorizationService, AttestationDefinitionService attestationDefinitionService) {
        super(authorizationService, attestationDefinitionService);
    }

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }
}
