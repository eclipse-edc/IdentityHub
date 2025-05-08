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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.ApiSchema;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Credential Request Status API as per DCP specification. " +
                "It serves endpoints to query the status of a credential issuance request from an issuer.",
                title = "Credential Request Status API",
                version = "v1alpha"))
@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID token containing an access_token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public interface CredentialRequestStatusApi {

    @Tag(name = "Credential Request Status API")
    @Operation(description = "Requests status information about an issuance request from an issuer",
            operationId = "getCredentialRequestStatus",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the status of a credentials request.",
                            content = @Content(schema = @Schema(implementation = ApiSchema.CredentialStatusSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. required parameter or properties were missing",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "401", description = "No Authorization header was provided.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "403", description = "The given authentication token could not be validated or the client is not authorized to call this endpoint.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "No credential request was found for the given ID.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject credentialStatus(String participantContextId, String credentialRequestId, String token);
}
