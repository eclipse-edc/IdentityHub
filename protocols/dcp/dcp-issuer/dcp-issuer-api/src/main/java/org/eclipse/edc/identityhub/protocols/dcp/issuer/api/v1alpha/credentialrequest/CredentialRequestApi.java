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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.ApiSchema;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Credential Request API as per DCP specification. It serves endpoints to request the issuance of Verifiable Credentials from an issuer.", title = "Credential Request API",
                version = "v1alpha"))
@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID token containing an access_token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public interface CredentialRequestApi {

    @Tag(name = "Credential Request API")
    @Operation(description = "Requests the issuance of one or several verifiable credentials from an issuer",
            operationId = "requestCredentials",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiSchema.CredentialRequestMessageSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The request was successfully received and is being processed.",
                            headers = @Header(
                                    name = "Location",
                                    description = "contains the relative URL where the status of the request can be queried (Credential Request Status API)",
                                    schema = @Schema(implementation = String.class)
                            )),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, e.g. required parameter or properties were missing",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "401", description = "No Authorization header was provided.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "403", description = "The given authentication token could not be validated or the client is not authorized to call this endpoint.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class))))

            }
    )
    Response requestCredential(String participantContextId, JsonObject message, String token);
}
