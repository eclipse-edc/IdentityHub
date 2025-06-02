/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.credentialoffer;


import io.swagger.v3.oas.annotations.ExternalDocumentation;
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

import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SPECIFICATION_URL;

@OpenAPIDefinition(
        info = @Info(description = "This implements the Credential Offer API as per DCP specification. It serves as notification facility for a holder.", title = "Credential Offer API",
                version = "1"))
@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public interface CredentialOfferApi {

    @Tag(name = "Credential Offer API")
    @Operation(description = "Notifies the holder about the availability of a particular credential for issuance",
            requestBody = @RequestBody(content = @Content(schema = @Schema(externalDocs = @ExternalDocumentation(description = "DCP Credential Offer API", url = DCP_SPECIFICATION_URL)))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The offer notification was successfully received."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "401", description = "No Authorization header was given.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "403", description = "The given authentication token could not be validated.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiSchema.ApiErrorDetailSchema.class))))

            }
    )
    void offerCredential(String participantContextId, JsonObject credentialOfferMessage, String authHeader);
}
