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

package org.eclipse.edc.identityhub.api.storage;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Storage API as per DCP specification. It serves endpoints to write VerifiableCredentials into storage.", title = "Storage API",
                version = "1"))
@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID token containing an access_token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public interface StorageApi {

    @Tag(name = "Storage API")
    @Operation(description = "Writes a set of credentials into storage",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiSchema.CredentialMessageSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The credentialMessage was successfully processed and stored"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "401", description = "No Authorization header was given.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "403", description = "The given authentication token could not be validated. This can happen, when the request body " +
                            "calls for a broader credentialMessage scope than the granted scope in the auth token",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class))))

            }
    )
    Response storeCredential(String participantContextId, JsonObject credentialMessage, String authHeader);
}
