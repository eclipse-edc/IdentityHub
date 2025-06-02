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

package org.eclipse.edc.identityhub.api.verifiablecredential.v1.unstable;

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
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;
import java.util.List;

@OpenAPIDefinition(info = @Info(description = "This is the Identity API for manipulating ParticipantContexts", title = "ParticipantContext Management API", version = "1"))
@Tag(name = "Participant Context")
public interface ParticipantContextApi {


    @Operation(description = "Creates a new ParticipantContext object.",
            operationId = "createParticipant",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The ParticipantContext was created successfully, its API token is returned in the response body.",
                            content = @Content(schema = @Schema(implementation = CreateParticipantContextResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't create the ParticipantContext, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    CreateParticipantContextResponse createParticipant(ParticipantManifest manifest);


    @Operation(description = "Gets ParticipantContexts by ID.",
            operationId = "getParticipant",
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
    ParticipantContext getParticipant(String participantContextId, SecurityContext securityContext);

    @Operation(description = "Regenerates the API token for a ParticipantContext and returns the new token.",
            operationId = "regenerateParticipantToken",
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
    String regenerateParticipantToken(String participantContextId, SecurityContext securityContext);

    @Operation(description = "Activates a ParticipantContext. This operation is idempotent, i.e. activating an already active ParticipantContext is a NOOP.",
            operationId = "activateParticipant",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantManifest.class), mediaType = "application/json")),
            parameters = {@Parameter(name = "isActive", description = "Whether the participantContext should be activated or deactivated. Defaults to 'false'")},
            responses = {
                    @ApiResponse(responseCode = "204", description = "The ParticipantContext was activated/deactivated successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void activateParticipant(String participantContextId, boolean isActive);

    @Operation(description = "Delete a ParticipantContext.",
            operationId = "deleteParticipant",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The ParticipantContext was deleted successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void deleteParticipant(String participantContextId, SecurityContext securityContext);

    @Operation(description = "Updates a ParticipantContext's roles. Note that this is an absolute update, that means all roles that the Participant should have must be submitted in the body. Requires elevated privileges.",
            operationId = "updateParticipantRoles",
            requestBody = @RequestBody(content = @Content(array = @ArraySchema(schema = @Schema(implementation = List.class)))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The ParticipantContext was updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A ParticipantContext with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void updateParticipantRoles(String participantContextId, List<String> roles);

    @Operation(description = "Get all DID documents across all Participant Contexts. Requires elevated access.",
            operationId = "getAllParticipants",
            parameters = {
                    @Parameter(name = "offset", description = "the paging offset. defaults to 0"),
                    @Parameter(name = "limit", description = "the page size. defaults to 50")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of ParticipantContexts.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipantContext.class)))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<ParticipantContext> getAllParticipants(Integer offset, Integer limit);
}
