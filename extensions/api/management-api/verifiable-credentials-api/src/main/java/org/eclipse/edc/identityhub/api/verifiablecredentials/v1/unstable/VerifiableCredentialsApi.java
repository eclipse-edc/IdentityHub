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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This is the Management API for VerifiableCredentials", title = "VerifiableCredentials Management API", version = "1"))
@Tag(name = "Verifiable Credentials")
public interface VerifiableCredentialsApi {


    @Operation(description = "Finds a VerifiableCredential by ID.",
            operationId = "getCredential",
            parameters = {
                    @Parameter(name = "participantId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The VerifiableCredential.",
                            content = @Content(schema = @Schema(implementation = ParticipantContext.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A VerifiableCredential with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    VerifiableCredentialResource getCredential(String id, SecurityContext securityContext);


    @Operation(description = "Query VerifiableCredentials by type.",
            operationId = "queryCredentialsByType",
            parameters = {
                    @Parameter(name = "participantId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of VerifiableCredentials.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DidDocument.class)))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<VerifiableCredentialResource> queryCredentialsByType(String type, SecurityContext securityContext);

    @Operation(description = "Delete a VerifiableCredential.",
            operationId = "deleteCredential",
            parameters = {
                    @Parameter(name = "participantId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The VerifiableCredential was deleted successfully", content = { @Content(schema = @Schema(implementation = String.class)) }),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A VerifiableCredential with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void deleteCredential(String id, SecurityContext securityContext);
}
