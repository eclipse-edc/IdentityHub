/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.v1;


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
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationResponse;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Presentation API as per IATP specification. It serves endpoints to query for specific VerifiablePresentations.", title = "Resolution API",
                version = "1"))
@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID token containing an access_token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public interface PresentationApi {

    @Tag(name = "Resolution API")
    @Operation(description = "Issues a new presentation query, that contains either a DIF presentation definition, or a list of scopes",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiSchema.PresentationQuerySchema.class), mediaType = "application/ld+json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The query was successfully processed, the response contains the VerifiablePresentation",
                            content = @Content(schema = @Schema(implementation = PresentationResponse.class), mediaType = "application/ld+json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, for example when both scope and presentationDefinition are given",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "No Authorization header was given.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The given authentication token could not be validated. This can happen, when the request body " +
                            "calls for a broader query scope than the granted scope in the auth token",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "501", description = "When the request contained a presentationDefinition object, but the implementation does not support it.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)), mediaType = "application/json"))
            }
    )
    Response queryPresentation(JsonObject query, String authHeader);
}
