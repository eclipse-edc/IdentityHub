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

package org.eclipse.edc.identityhub.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.RequestObject;
import org.eclipse.edc.identityhub.spi.model.RequestStatus;
import org.eclipse.edc.identityhub.spi.model.ResponseObject;
import org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod;
import org.eclipse.edc.identityhub.spi.model.WebNodeInterfaces;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessorRegistry;

import java.util.stream.Collectors;

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

    public IdentityHubController(MessageProcessorRegistry messageProcessorRegistry) {
        this.messageProcessorRegistry = messageProcessorRegistry;
    }

    @Operation(description = "A Decentralized Web Node (https://identity.foundation/decentralized-web-node/spec) compatible endpoint supporting operations to read and write Verifiable Credentials into an Identity Hub")
    @POST
    public ResponseObject handleRequest(RequestObject requestObject) {
        var replies = requestObject.getMessages()
                .stream()
                .map(this::processMessage)
                .collect(Collectors.toList());

        return ResponseObject.Builder.newInstance()
                .status(RequestStatus.OK)
                .replies(replies)
                .build();
    }

    private MessageResponseObject processMessage(MessageRequestObject messageRequestObject) {
        var method = WebNodeInterfaceMethod.fromName(messageRequestObject.getDescriptor().getMethod());
        var processor = messageProcessorRegistry.resolve(method);
        return processor.process(messageRequestObject);
    }

}

