/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.ParticipantDto;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate participants in an Issuer Service", title = "Issuer Service Participant Admin API", version = "1"))
@Tag(name = "Issuer Service Participant Admin API")
public interface IssuerParticipantAdminApi {


    @Operation(description = "Adds a new participant.",
            operationId = "addParticipant",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantDto.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The participant was added successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't add the participant, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response addParticipant(ParticipantDto participant);

    @Operation(description = "Updates participant data.",
            operationId = "updateParticipant",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ParticipantDto.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The participant was updated successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Can't update the participant because it was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response updateParticipant(ParticipantDto participant);

    @Operation(description = "Gets metadata for a certain participant.",
            operationId = "getParticipantById",
            parameters = {@Parameter(name = "participantId", description = "ID of the participant who should be returned", required = true, in = ParameterIn.PATH)},
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of verifiable credential metadata. Note that these are not actual VerifiableCredentials.",
                            content = @Content(schema = @Schema(implementation = Participant.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    ParticipantDto getParticipantById(String participantId);

    @Operation(description = "Gets all participants for a certain query.",
            operationId = "queryParticipants",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of participant metadata.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipantDto.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Collection<ParticipantDto> queryParticipants(QuerySpec querySpec);
}
