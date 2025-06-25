/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Amadeus IT Group - adds endpoints to create and updates verifiable credentials
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.verifiablecredential.validation.VerifiableCredentialManifestValidator;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model.CredentialRequestDto;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model.HolderCredentialRequestDto;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/credentials")
public class VerifiableCredentialsApiController implements VerifiableCredentialsApi {

    private final CredentialStore credentialStore;
    private final AuthorizationService authorizationService;
    private final VerifiableCredentialManifestValidator validator;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final CredentialRequestManager credentialRequestService;

    public VerifiableCredentialsApiController(CredentialStore credentialStore,
                                              AuthorizationService authorizationService,
                                              VerifiableCredentialManifestValidator validator,
                                              TypeTransformerRegistry typeTransformerRegistry,
                                              CredentialRequestManager credentialRequestService) {
        this.credentialStore = credentialStore;
        this.authorizationService = authorizationService;
        this.validator = validator;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.credentialRequestService = credentialRequestService;
    }

    @GET
    @Path("/{credentialId}")
    @Override
    public VerifiableCredentialResource getCredential(@PathParam("participantContextId") String participantId, @PathParam("credentialId") String id, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, id, VerifiableCredentialResource.class)
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class, id));

        var result = credentialStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", id)).build())
                .orElseThrow(InvalidRequestException::new);
        return result.stream().findFirst().orElseThrow(() -> new ObjectNotFoundException(VerifiableCredentialResource.class, id));
    }

    @POST
    @Override
    public void addCredential(@PathParam("participantContextId") String participantId, VerifiableCredentialManifest manifest, @Context SecurityContext securityContext) {
        validator.validate(manifest).orElseThrow(ValidationFailureException::new);

        var decoded = onEncoded(participantId).orElseThrow(InvalidRequestException::new);
        authorizationService.isAuthorized(securityContext, decoded, ParticipantContext.class)
                .compose(u -> typeTransformerRegistry.transform(manifest, VerifiableCredentialResource.class)
                        .map(ServiceResult::success)
                        .orElse(failure -> badRequest(failure.getFailureDetail())))
                .compose(vcr -> ServiceResult.from(credentialStore.create(vcr)))
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class));
    }

    @PUT
    @Override
    public void updateCredential(@PathParam("participantContextId") String participantId, VerifiableCredentialManifest manifest, @Context SecurityContext securityContext) {
        validator.validate(manifest).orElseThrow(ValidationFailureException::new);

        authorizationService.isAuthorized(securityContext, manifest.getId(), VerifiableCredentialResource.class)
                .compose(u -> typeTransformerRegistry.transform(manifest, VerifiableCredentialResource.class)
                        .map(ServiceResult::success)
                        .orElse(failure -> badRequest(failure.getFailureDetail())))
                .compose(vcr -> ServiceResult.from(credentialStore.update(vcr)))
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class));
    }

    @GET
    @Override
    public Collection<VerifiableCredentialResource> queryCredentialsByType(@PathParam("participantContextId") String participantId, @Nullable @QueryParam("type") String type, @Context SecurityContext securityContext) {
        var query = QuerySpec.Builder.newInstance();

        if (!StringUtils.isNullOrEmpty(type)) {
            query.filter(new Criterion("verifiableCredential.credential.type", "contains", type));
        }

        return credentialStore.query(query.build())
                .orElseThrow(InvalidRequestException::new)
                .stream()
                .filter(vcr -> authorizationService.isAuthorized(securityContext, vcr.getId(), VerifiableCredentialResource.class).succeeded())
                .toList();
    }

    @DELETE
    @Path("/{credentialId}")
    @Override
    public void deleteCredential(@PathParam("participantContextId") String participantId, @PathParam("credentialId") String id, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, id, VerifiableCredentialResource.class)
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class, id));
        var res = credentialStore.deleteById(id);
        if (res.failed()) {
            throw exceptionMapper(VerifiableCredentialResource.class, id).apply(ServiceResult.fromFailure(res).getFailure());
        }
    }

    @POST
    @Path("/request")
    @Override
    public Response requestCredential(@PathParam("participantContextId") String participantContextId, CredentialRequestDto credentialRequestDto, @Context SecurityContext securityContext) {
        var participantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        authorizationService.isAuthorized(securityContext, participantId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));

        var holderPid = ofNullable(credentialRequestDto.holderPid()).orElseGet(() -> UUID.randomUUID().toString());
        var requestParameters = credentialRequestDto.credentials().stream().map(cd -> new RequestedCredential(cd.id(), cd.type(), cd.format())).toList();

        return credentialRequestService.initiateRequest(participantId, credentialRequestDto.issuerDid(), holderPid, requestParameters)
                .map(id -> Response.created(URI.create(Versions.UNSTABLE + "/participants/%s/credentials/request/%s".formatted(participantContextId, id))).build())
                .orElseThrow(exceptionMapper(CredentialRequestDto.class));
    }

    @GET
    @Path("/request/{holderPid}")
    @Override
    public HolderCredentialRequestDto getCredentialRequest(@PathParam("participantContextId") String participantContextId,
                                                           @PathParam("holderPid") String holderPid,
                                                           @Context SecurityContext securityContext) {

        var participantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        authorizationService.isAuthorized(securityContext, participantId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));

        return ofNullable(credentialRequestService.findById(holderPid))
                .map(req -> new HolderCredentialRequestDto(req.getIssuerDid(), req.getHolderPid(), req.getIssuerPid(), req.stateAsString(), req.getIdsAndFormats()))
                .orElseThrow(() -> new ObjectNotFoundException(HolderCredentialRequest.class, holderPid));
    }

}
