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

package org.eclipse.edc.identityhub.api.participantcontext.v1;

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
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.web.spi.ApiErrorDetail;

@OpenAPIDefinition(info = @Info(description = "This is the Management API for ParticipantContexts", title = "ParticipantContext Management API", version = "1"))
public interface ParticipantContextApi {

    @Tag(name = "ParticipantContext Management API")
    @Operation(description = "Creates a new ParticipantContext object.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ParticipantContext was created successfully, its API token is returned in the response body."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't create the ParticipantContext, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    String createParticipant(ParticipantManifest manifest);


    @Tag(name = "ParticipantContext Management API")
    @Operation(description = "Gets ParticipantContexts by ID.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of ParticipantContexts.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    ParticipantContext getParticipant(String participantId, SecurityContext securityContext);

    @Tag(name = "ParticipantContext Management API")
    @Operation(description = "Regenerates the API token for a ParticipantContext and returns the new token.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The API token was regenerated successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    String regenerateToken(String participantId, SecurityContext securityContext);

    @Tag(name = "ParticipantContext Management API")
    @Operation(description = "Activates a ParticipantContext. This operation is idempotent, i.e. activating an already active ParticipantContext is a NOOP.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            parameters = {@Parameter(name = "isActive", description = "Whether the participantContext should be activated or deactivated. Defaults to 'false'")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ParticipantContext was activated/deactivated successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void activateParticipant(String participantId, boolean isActive);

    @Tag(name = "ParticipantContext Management API")
    @Operation(description = "Delete a ParticipantContext.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ParticipantContext was deleted successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void deleteParticipant(String participantId, SecurityContext securityContext);
}
