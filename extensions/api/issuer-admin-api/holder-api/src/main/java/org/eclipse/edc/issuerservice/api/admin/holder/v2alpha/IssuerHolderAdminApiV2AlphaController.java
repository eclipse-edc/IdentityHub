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

package org.eclipse.edc.issuerservice.api.admin.holder.v2alpha;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.IssuerHolderAdminApiController;
import org.eclipse.edc.issuerservice.api.admin.holder.v2alpha.model.HolderPatchDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/holders")
public class IssuerHolderAdminApiV2AlphaController extends IssuerHolderAdminApiController implements IssuerHolderAdminApiV2Alpha {

    public IssuerHolderAdminApiV2AlphaController(AuthorizationService authorizationService, HolderService holderService) {
        super(authorizationService, holderService);
    }

    @PATCH
    @Path("/{holderId}")
    @RequiredScope("issuer-admin-api:holders:write")
    @Override
    public Holder patchHolder(@PathParam("participantContextId") String participantContextId, @PathParam("holderId") String holderId, HolderPatchDto patch, @Context SecurityContext context) {
        if (patch == null) {
            throw new InvalidRequestException("Request body is missing.");
        }
        patch.validate().orElseThrow(failure -> new InvalidRequestException(failure.getMessages()));

        return authorizationService.authorize(context, participantContextId, holderId, Holder.class)
                .compose(u -> holderService.patchHolder(participantContextId, holderId, patch.did(), patch.name(), patch.properties(), patch.removed()))
                .orElseThrow(exceptionMapper(Holder.class, holderId));
    }

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }
}
