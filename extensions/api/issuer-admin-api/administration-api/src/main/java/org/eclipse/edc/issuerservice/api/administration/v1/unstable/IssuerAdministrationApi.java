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

package org.eclipse.edc.issuerservice.api.administration.v1.unstable;

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
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.issuerservice.api.administration.v1.unstable.model.RotationRequest;
import org.eclipse.edc.web.spi.ApiErrorDetail;

@OpenAPIDefinition(info = @Info(description = "This is the Administration API for the Issuer Service. Please make sure to only allow access for authorized users.", title = "Issuer Service Administration API", version = "1"))
@Tag(name = "Issuer Service Administration API")
public interface IssuerAdministrationApi {

    // todo: can these endpoints be replaced with an IdentityHub next to the IssuerService?

    @Operation(description = "Revokes a particular key pair, identified by their ID and create a new successor key.",
            operationId = "revokeKeyPair",
            parameters = {
                    @Parameter(name = "keyPairId", description = "ID of the key pair to revoke", required = true, in = ParameterIn.PATH)
            },
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The key pair was successfully revoked."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A key pair with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Response revokeKeyPair(String keyPairId, KeyDescriptor newKey);

    @Operation(description = "Rotates (=retires) a particular key pair, identified by their ID and optionally create a new successor key.",
            operationId = "rotateKeyPair",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = RotationRequest.class), mediaType = "application/json")),
            parameters = {
                    @Parameter(name = "keyId", description = "ID of the key pair to rotate"),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ID of the successor key pair."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "A key pair with the given ID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    String initiateKeyRotation(String keyId, RotationRequest request);

    @Operation(description = "Adds a new key pair to the issuer service. Note that the key pair is either generated, or the private key is expected to be found in the vault.",
            operationId = "addKeyPair",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = KeyDescriptor.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The key pair was successfully added."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))

            }
    )
    Response addKeyPair(KeyDescriptor request);


    @Operation(description = "Finds a KeyPairResource by ID.",
            operationId = "getKeyPair",
            parameters = {
                    @Parameter(name = "keyPairId", description = "The ID of the KeyPair to get", required = true, in = ParameterIn.PATH)
            },
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
    KeyPairResource getKeyPair(String keyPairId);


}
