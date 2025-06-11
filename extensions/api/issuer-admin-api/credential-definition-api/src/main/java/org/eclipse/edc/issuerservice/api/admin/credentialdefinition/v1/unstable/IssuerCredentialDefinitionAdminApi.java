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

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.model.CredentialDefinitionDto;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate credential definitions in an Issuer Service", title = "Issuer Service Credential Definitions Admin API", version = "1"))
@Tag(name = "Issuer Service Credential Definition Admin API")
public interface IssuerCredentialDefinitionAdminApi {


    @Operation(description = "Adds a new credential definition.",
            operationId = "createCredentialDefinition",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = CredentialDefinitionDto.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The credential definition was created successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't create the credential definition, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response createCredentialDefinition(String participantContextId, CredentialDefinitionDto credentialDefinition, SecurityContext context);

    @Operation(description = "Updates credential definition.",
            operationId = "updateCredentialDefinition",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = CredentialDefinitionDto.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The credential definition was updated successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Can't update the credential definition because it was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response updateCredentialDefinition(String participantContextId, CredentialDefinitionDto credentialDefinition, SecurityContext context);

    @Operation(description = "Gets a credential definition by its ID.",
            operationId = "getCredentialDefinitionById",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The credential definition was found.",
                            content = @Content(schema = @Schema(implementation = CredentialDefinition.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential definition was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    CredentialDefinition getCredentialDefinitionById(String participantContextId, String credentialDefinitionId, SecurityContext securityContext);

    @Operation(description = "Gets all credential definitions for a certain query.",
            operationId = "queryCredentialDefinitions",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of credentials definitions.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialDefinition.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<CredentialDefinition> queryCredentialDefinitions(String participantContextId, QuerySpec querySpec, SecurityContext context);


    @Operation(description = "Deletes a credential definition by its ID.",
            operationId = "deleteCredentialDefinitionById",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The credential definition was deleted.",
                            content = @Content(schema = @Schema(implementation = CredentialDefinition.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential definition was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            })
    void deleteCredentialDefinitionById(String participantContextId, String credentialDefinitionId, SecurityContext context);

}
