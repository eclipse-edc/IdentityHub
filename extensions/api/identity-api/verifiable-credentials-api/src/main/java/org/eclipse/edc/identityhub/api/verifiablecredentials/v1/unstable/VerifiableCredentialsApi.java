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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable;

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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model.CredentialRequestDto;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model.HolderCredentialRequestDto;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This is the Identity API for manipulating VerifiableCredentials", title = "VerifiableCredentials Identity API", version = "1"))
@Tag(name = "Verifiable Credentials")
public interface VerifiableCredentialsApi {


    @Operation(description = "Finds a VerifiableCredential by ID.",
            operationId = "getCredential",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The VerifiableCredential.",
                            content = @Content(schema = @Schema(implementation = VerifiableCredentialResource.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A VerifiableCredential with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    VerifiableCredentialResource getCredential(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Adds a new VerifiableCredential into the system.",
            operationId = "addCredential",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = VerifiableCredentialManifest.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The VerifiableCredential was successfully created."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Could not create VerifiableCredential, because a VerifiableCredential with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void addCredential(String participantContextId, VerifiableCredentialManifest manifest, SecurityContext securityContext);

    @Operation(description = "Update an existing VerifiableCredential.",
            operationId = "updateCredential",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = VerifiableCredentialManifest.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The VerifiableCredential was updated successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "VerifiableCredential could not be updated because it does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void updateCredential(String participantContextId, VerifiableCredentialManifest manifest, SecurityContext securityContext);


    @Operation(description = "Query VerifiableCredentials by type.",
            operationId = "queryCredentialsByType",
            parameters = {
                    @Parameter(name = "type", description = "Credential type. If omitted, all credentials are returned (limited to 50 elements).")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of VerifiableCredentials.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = VerifiableCredentialResource.class)))),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<VerifiableCredentialResource> queryCredentialsByType(String participantContextId, String type, SecurityContext securityContext);

    @Operation(description = "Delete a VerifiableCredential.",
            operationId = "deleteCredential",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The VerifiableCredential was deleted successfully", content = {@Content(schema = @Schema(implementation = String.class))}),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A VerifiableCredential with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void deleteCredential(String participantContextId, String id, SecurityContext securityContext);

    @Operation(description = "Triggers a credential request that is send to the issuer via the DCP protocol.",
            operationId = "requestCredential",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = CredentialRequestDto.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The request was processed and sent to the issuer. The issuer-created ID (\"issuerPid\") is returned in the response."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Could not create a credential request, because a credential request with that ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response requestCredential(String participantContextId, CredentialRequestDto credentialRequestDto, SecurityContext securityContext);

    @Operation(description = "Finds a credential request by ID.",
            operationId = "getCredentialRequest",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The VerifiableCredential.",
                            content = @Content(schema = @Schema(implementation = VerifiableCredentialResource.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A VerifiableCredential with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    HolderCredentialRequestDto getCredentialRequest(String participantContextId, String holderPid, SecurityContext securityContext);
}
