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

package org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable.model.IssuanceProcessDto;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/issuanceprocesses")
public class IssuanceProcessAdminApiController implements IssuanceProcessAdminApi {

    private final IssuanceProcessService issuanceProcessService;
    private final AuthorizationService authorizationService;

    public IssuanceProcessAdminApiController(IssuanceProcessService issuanceProcessService, AuthorizationService authorizationService) {
        this.issuanceProcessService = issuanceProcessService;
        this.authorizationService = authorizationService;
    }

    @GET
    @Path("/{issuanceProcessId}")
    @Override
    public IssuanceProcessDto getIssuanceProcessById(@PathParam("participantContextId") String participantContextId,
                                                     @PathParam("issuanceProcessId") String issuanceProcessId,
                                                     @Context SecurityContext securityContext) {

        authorizationService.isAuthorized(securityContext, issuanceProcessId, IssuanceProcess.class)
                .orElseThrow(exceptionMapper(IssuanceProcess.class, issuanceProcessId));

        var issuanceProcess = issuanceProcessService.findById(issuanceProcessId);

        if (issuanceProcess == null) {
            throw new ObjectNotFoundException(IssuanceProcess.class, issuanceProcessId);
        }
        return IssuanceProcessDto.fromIssuanceProcess(issuanceProcess);
    }

    @POST
    @Path("/query")
    @Override
    public Collection<IssuanceProcessDto> queryIssuanceProcesses(@PathParam("participantContextId") String participantContextId, QuerySpec querySpec, @Context SecurityContext securityContext) {
        return onEncoded(participantContextId).map(decoded -> {
            var spec = querySpec.toBuilder().filter(filterByParticipantContextId(decoded)).build();
            return issuanceProcessService.search(spec)
                    .orElseThrow(exceptionMapper(IssuanceProcess.class, null))
                    .stream()
                    .filter(issuanceProcess -> authorizationService.isAuthorized(securityContext, issuanceProcess.getId(), IssuanceProcess.class).succeeded())
                    .map(IssuanceProcessDto::fromIssuanceProcess)
                    .collect(toList());
        }).orElseThrow(InvalidRequestException::new);

    }

}
