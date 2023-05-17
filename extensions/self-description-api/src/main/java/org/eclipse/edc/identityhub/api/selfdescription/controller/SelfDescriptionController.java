/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.selfdescription.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/identity-hub")
public class SelfDescriptionController implements SelfDescriptionApi {

    private final JsonNode selfDescription;

    public SelfDescriptionController(JsonNode selfDescription) {
        this.selfDescription = selfDescription;
    }

    @GET
    @Override
    @Path("/self-description")
    public JsonNode getSelfDescription() {
        return selfDescription;
    }
}

