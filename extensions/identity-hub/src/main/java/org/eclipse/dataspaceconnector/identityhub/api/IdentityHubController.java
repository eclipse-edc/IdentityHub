/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.identityhub.model.MessageRequestObject;
import org.eclipse.dataspaceconnector.identityhub.model.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.model.RequestObject;
import org.eclipse.dataspaceconnector.identityhub.model.RequestStatus;
import org.eclipse.dataspaceconnector.identityhub.model.ResponseObject;
import org.eclipse.dataspaceconnector.identityhub.model.WebNodeInterfaces;
import org.eclipse.dataspaceconnector.identityhub.processor.MessageProcessorRegistry;

import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.identityhub.model.WebNodeInterfaceMethod.fromName;

/**
 * Identity Hub controller, exposing a <a href="https://identity.foundation/decentralized-web-node/spec">Decentralized Web Node</a> compatible endpoint.
 * <p>
 * See {@link WebNodeInterfaces} for a list of currently supported DWN interfaces.
 */
@Tag(name = "IdentityHub")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/identity-hub")
public class IdentityHubController {

    private final MessageProcessorRegistry messageProcessorRegistry;
    private final JsonNode selfDescription;

    public IdentityHubController(MessageProcessorRegistry messageProcessorRegistry, JsonNode selfDescription) {
        this.messageProcessorRegistry = messageProcessorRegistry;
        this.selfDescription = selfDescription;
    }

    @Operation(description = "A Decentralized Web Node (https://identity.foundation/decentralized-web-node/spec) compatible endpoint supporting operations to read and write Verifiable Credentials into an Identity Hub")
    @POST
    public ResponseObject handleRequest(RequestObject requestObject) {
        var replies = requestObject.getMessages()
                .stream()
                .map(this::processMessage)
                .collect(Collectors.toList());

        return ResponseObject.Builder.newInstance()
                .requestId(requestObject.getRequestId())
                .status(RequestStatus.OK)
                .replies(replies)
                .build();
    }

    @Operation(description = "Serve Self-Description document as defined in Gaia-X Trust Framework (https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/)")
    @GET
    @Path("/self-description")
    public JsonNode getSelfDescription() {
        return selfDescription;
    }

    private MessageResponseObject processMessage(MessageRequestObject messageRequestObject) {
        var method = fromName(messageRequestObject.getDescriptor().getMethod());
        var processor = messageProcessorRegistry.resolve(method);
        return processor.process(messageRequestObject.getData());
    }

}

