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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/keypairs")
public class KeyPairResourceApiController implements KeyPairResourceApi {


    private final KeyPairService keyPairService;

    public KeyPairResourceApiController(KeyPairService keyPairService) {

        this.keyPairService = keyPairService;
    }

    @GET
    @Path("/{keyPairId}")
    @Override
    public KeyPairResource findById(@PathParam("keyPairId") String id) {
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
    public Collection<KeyPairResource> findForParticipant(@QueryParam("participantId") String participantId) {
        var query = QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", participantId)).build();
        return keyPairService.query(query).orElseThrow(exceptionMapper(KeyPairResource.class, participantId));
    }

    @PUT
    @Override
    public void addKeyPair(@QueryParam("participantId") String participantId, KeyDescriptor keyDescriptor, @QueryParam("makeDefault") boolean makeDefault) {
        keyPairService.addKeyPair(participantId, keyDescriptor, makeDefault)
                .orElseThrow(exceptionMapper(KeyPairResource.class));
    }

    @POST
    @Path("/{keyPairId}/rotate")
    @Override
    public void rotateKeyPair(@PathParam("keyPairId") String id, KeyDescriptor newKey, @QueryParam("duration") long duration) {
        keyPairService.rotateKeyPair(id, newKey, duration)
                .orElseThrow(exceptionMapper(KeyPairResource.class, id));
    }

    @POST
    @Path("/{keyPairId}/revoke")
    @Override
    public void revokeKey(@PathParam("keyPairId") String id, KeyDescriptor newKey) {
        keyPairService.revokeKey(id, newKey)
                .orElseThrow(exceptionMapper(KeyPairResource.class, id));
    }
}
