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
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This is the Management API for KeyPairResources", title = "KeyPairResources Management API", version = "1"))
public interface KeyPairResourceApi {

    @Tag(name = "KeyPairResources Management API")
    @Operation(description = "Finds a KeyPairResource by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    KeyPairResource findById(String id, SecurityContext securityContext);

    @Tag(name = "KeyPairResources Management API")
    @Operation(description = "Finds all KeyPairResources for a particular ParticipantContext.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Collection<KeyPairResource> findForParticipant(String participantId, SecurityContext securityContext);

    @Tag(name = "KeyPairResources Management API")
    @Operation(description = "Adds a new key pair to a ParticipantContext. Note that the key pair is either generated, or the private key is expected to be found in the vault.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            parameters = @Parameter(name = "makeDefault", description = "Make the new key pair the default key pair"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource was successfully created and linked to the participant.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void addKeyPair(String participantId, KeyDescriptor keyDescriptor, boolean makeDefault, SecurityContext securityContext);

    @Tag(name = "KeyPairResources Management API")
    @Operation(description = "Rotates (=retires) a particular key pair, identified by their ID and optionally create a new successor key.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            parameters = @Parameter(name = "duration", description = "Indicates for how long the public key of the rotated/retired key pair should still be available "),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource was successfully rotated and linked to the participant.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void rotateKeyPair(String id, KeyDescriptor newKey, long duration, SecurityContext securityContext);

    @Tag(name = "KeyPairResources Management API")
    @Operation(description = "Revokes (=removes) a particular key pair, identified by their ID and create a new successor key.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The KeyPairResource was successfully rotated and linked to the participant.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A KeyPairResource with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void revokeKey(String id, KeyDescriptor newKey, SecurityContext securityContext);
}
