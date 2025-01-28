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
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.AttestationRequest;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.AttestationResponse;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate attestations for participants in an Issuer Service", title = "Issuer Service Attestation Admin API", version = "1"))
@Tag(name = "Issuer Service Attestation Admin API")
public interface IssuerAttestationAdminApi {

    @Operation(description = "Creates an attestation in the runtime.",
            operationId = "createAttestation",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = AttestationRequest.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The attestation was added successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't add the participant, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response createAttestation(AttestationRequest attestationRequest);

    @Operation(description = "Links an attestation to a participant.",
            operationId = "linkAttestation",
            parameters = {
                    @Parameter(name = "participantId", description = "ID of the participant", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "attestationId", description = "ID of the attestation", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "The attestation was added successfully."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't add the participant, because a object with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response linkAttestation(String participantId, String attestationId);

    @Operation(description = "Deletes a particular attestation for a given participant.",
            operationId = "deleteAttestation",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            parameters = {
                    @Parameter(name = "participantId", description = "ID of the participant", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "attestationId", description = "ID of the attestation to be removed", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "The attestation was successfully deleted."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The attestation or the participant was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response deleteAttestation(String participantId, String attestationId);

    @Operation(description = "Get all attestations for a given participant.",
            operationId = "getAttestations",
            parameters = {
                    @Parameter(name = "participantId", description = "ID of the participant", required = true, in = ParameterIn.PATH)
            },
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of attestation metadata.",
                            content = @Content(schema = @Schema(implementation = AttestationResponse.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    AttestationResponse getAttestations(String participantId);

    @Operation(description = "Query attestations, possibly across multiple participants.",
            operationId = "queryAttestations",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of attestation metadata.",
                            content = @Content(schema = @Schema(implementation = AttestationResponse.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    AttestationResponse queryAttestations(QuerySpec query);
}
