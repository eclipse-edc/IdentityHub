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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This is the Identity API for manipulating KeyPairResources", title = "KeyPairResources Identity API", version = "1"))
@Tag(name = "Key Pairs")
public interface KeyPairResourceApi {

    @Operation(description = "Finds a KeyPairResource by ID.",
            operationId = "getKeyPair",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource.",
                            content = @Content(schema = @Schema(implementation = KeyPairResource.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    KeyPairResource getKeyPair(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Finds all KeyPairResources for a particular ParticipantContext.",
            operationId = "queryKeyPairByParticipantId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KeyPairResource.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Collection<KeyPairResource> queryKeyPairByParticipantId(String participantContextId, SecurityContext securityContext);

    @Operation(description = "Adds a new key pair to a ParticipantContext. Note that the key pair is either generated, or the private key is expected to be found in the vault.",
            operationId = "addKeyPair",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            parameters = @Parameter(name = "makeDefault", description = "Make the new key pair the default key pair"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The KeyPairResource was successfully created and linked to the participant."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void addKeyPair(String participantContextId, KeyDescriptor keyDescriptor, boolean makeDefault, SecurityContext securityContext);


    @Operation(description = "Sets a KeyPairResource to the ACTIVE state. Will fail if the current state is anything other than ACTIVE or CREATED.",
            operationId = "activateKeyPair",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The KeyPairResource."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void activateKeyPair(String participantContextId, String keyPairResourceId, SecurityContext securityContext);

    @Operation(description = "Rotates (=retires) a particular key pair, identified by their ID and optionally create a new successor key.",
            operationId = "rotateKeyPair",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            parameters = {
                    @Parameter(name = "duration", description = "Indicates for how long the public key of the rotated/retired key pair should still be available "),
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "The KeyPairResource was successfully rotated and linked to the participant."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void rotateKeyPair(String participantContextId, String id, KeyDescriptor newKey, long duration, SecurityContext securityContext);

    @Operation(description = "Revokes (=removes) a particular key pair, identified by their ID and create a new successor key.",
            operationId = "revokeKeyPair",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The KeyPairResource was successfully rotated and linked to the participant."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void revokeKeyPair(String participantContextId, String id, KeyDescriptor newKey, SecurityContext securityContext);


}
