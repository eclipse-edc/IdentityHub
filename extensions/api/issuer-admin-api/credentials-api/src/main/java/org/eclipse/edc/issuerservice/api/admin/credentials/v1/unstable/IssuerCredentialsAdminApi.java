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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable;

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
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialStatusResponse;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialDto;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate VerifiableCredentials for participants in an Issuer Service", title = "Issuer Service Credentials Admin API", version = "1"))
@Tag(name = "Issuer Service Credentials Admin API")
public interface IssuerCredentialsAdminApi {


    @Operation(description = "Query credentials, possibly across multiple participants.",
            operationId = "queryCredentials",
            parameters = {
                    @Parameter(name = "participantContextId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH)
            },
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of verifiable credential metadata. Note that these are not actual VerifiableCredentials.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = VerifiableCredentialDto.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Collection<VerifiableCredentialDto> queryCredentials(String participantContextId, QuerySpec query, SecurityContext context);


    @Operation(description = "Revokes a credential with the given ID for the given participant. Revoked credentials will be added to the Revocation List",
            operationId = "revokeCredential",
            parameters = {
                    @Parameter(name = "participantContextId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "credentialId", description = "ID of the credential to revoke", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "The credential was revoked successfully. Check the Revocation List credential to confirm."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential or the participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response revokeCredential(String participantContextId, String credentialId, SecurityContext context);


    @Operation(description = "Suspends a credential with the given ID for the given participant. Suspended credentials will be added to the Revocation List. Suspension is reversible.",
            operationId = "suspendCredential",
            parameters = {
                    @Parameter(name = "participantContextId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "credentialId", description = "ID of the credential to revoke", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "The credential was suspended successfully. Check the Revocation List credential to confirm."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential or the participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response suspendCredential(String participantContextId, String credentialId, SecurityContext context);

    @Operation(description = "Resumes a credential with the given ID for the given participant. Resumed credentials will be removed from the Revocation List.",
            operationId = "resumeCredential",
            parameters = {
                    @Parameter(name = "participantContextId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "credentialId", description = "ID of the credential to resume", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "The credential was resumed successfully. Check the Revocation List credential to confirm."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential or the participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response resumeCredential(String participantContextId, String credentialId, SecurityContext context);

    @Operation(description = "Checks the revocation status of a credential with the given ID for the given participant.",
            operationId = "checkCredentialStatus",
            parameters = {
                    @Parameter(name = "participantContextId", description = "Base64-Url encode Participant Context ID", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "credentialId", description = "ID of the credential to check", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The credential status."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The credential or the participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    CredentialStatusResponse checkRevocationStatus(String participantContextId, String credentialId, SecurityContext context);
}
