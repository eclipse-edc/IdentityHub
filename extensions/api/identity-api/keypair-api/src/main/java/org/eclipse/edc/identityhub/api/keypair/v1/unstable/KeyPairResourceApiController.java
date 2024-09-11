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
 *
 */

package org.eclipse.edc.identityhub.api.keypair.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.v1.validation.KeyDescriptorValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantId}/keypairs")
public class KeyPairResourceApiController implements KeyPairResourceApi {

    private final AuthorizationService authorizationService;
    private final KeyPairService keyPairService;
    private final KeyDescriptorValidator keyDescriptorValidator;

    public KeyPairResourceApiController(AuthorizationService authorizationService, KeyPairService keyPairService, KeyDescriptorValidator keyDescriptorValidator) {
        this.authorizationService = authorizationService;
        this.keyPairService = keyPairService;
        this.keyDescriptorValidator = keyDescriptorValidator;
    }

    @GET
    @Path("/{keyPairId}")
    @Override
    public KeyPairResource getKeyPair(@PathParam("keyPairId") String id, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, id, KeyPairResource.class).orElseThrow(exceptionMapper(KeyPairResource.class, id));

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", id)).build();
        var result = keyPairService.query(query).orElseThrow(exceptionMapper(KeyPairResource.class, id));
        if (result.isEmpty()) {
            throw new ObjectNotFoundException(KeyPairResource.class, id);
        }
        if (result.size() > 1) {
            throw new EdcException("Expected only 1 result, but got %s".formatted(result.size()));
        }
        return result.iterator().next();
    }

    @GET
    @Override
    public Collection<KeyPairResource> queryKeyPairByParticipantId(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        return onEncoded(participantId).map(decoded -> {
            var query = ParticipantResource.queryByParticipantId(decoded).build();
            return keyPairService.query(query).orElseThrow(exceptionMapper(KeyPairResource.class, decoded)).stream().filter(kpr -> authorizationService.isAuthorized(securityContext, kpr.getId(), KeyPairResource.class).succeeded()).toList();
        }).orElseThrow(InvalidRequestException::new);
    }

    @PUT
    @Override
    public void addKeyPair(@PathParam("participantId") String participantId, KeyDescriptor keyDescriptor, @QueryParam("makeDefault") boolean makeDefault, @Context SecurityContext securityContext) {
        keyDescriptorValidator.validate(keyDescriptor).orElseThrow(ValidationFailureException::new);
        onEncoded(participantId)
                .onSuccess(decoded ->
                        authorizationService.isAuthorized(securityContext, decoded, ParticipantContext.class)
                                .compose(u -> keyPairService.addKeyPair(decoded, keyDescriptor, makeDefault))
                                .orElseThrow(exceptionMapper(KeyPairResource.class)))
                .orElseThrow(InvalidRequestException::new);
    }

    @POST
    @Path("/{keyPairId}/activate")
    @Override
    public void activateKeyPair(@PathParam("keyPairId") String keyPairResourceId, @Context SecurityContext context) {
        authorizationService.isAuthorized(context, keyPairResourceId, KeyPairResource.class)
                .compose(u -> keyPairService.activate(keyPairResourceId)).orElseThrow(exceptionMapper(KeyPairResource.class, keyPairResourceId));

    }

    @POST
    @Path("/{keyPairId}/rotate")
    @Override
    public void rotateKeyPair(@PathParam("keyPairId") String keyPairId, @Nullable KeyDescriptor newKey, @QueryParam("duration") long duration, @Context SecurityContext securityContext) {
        if (newKey != null) {
            keyDescriptorValidator.validate(newKey).orElseThrow(ValidationFailureException::new);
        }
        authorizationService.isAuthorized(securityContext, keyPairId, KeyPairResource.class).compose(u -> keyPairService.rotateKeyPair(keyPairId, newKey, duration)).orElseThrow(exceptionMapper(KeyPairResource.class, keyPairId));
    }

    @POST
    @Path("/{keyPairId}/revoke")
    @Override
    public void revokeKeyPair(@PathParam("keyPairId") String id, KeyDescriptor newKey, @Context SecurityContext securityContext) {
        if (newKey != null) {
            keyDescriptorValidator.validate(newKey).orElseThrow(ValidationFailureException::new);
        }
        authorizationService.isAuthorized(securityContext, id, KeyPairResource.class).compose(u -> keyPairService.revokeKey(id, newKey)).orElseThrow(exceptionMapper(KeyPairResource.class, id));
    }
}
