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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.issuermetadata;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.ApiSchema;

@OpenAPIDefinition(
        info = @Info(description = "This API provides information about the capabilities of this issuer. " +
                "Specifically, it provides information about supported credentials, profiles and binding methods." +
                "This API is publicly available without Authentication.",
                title = "Issuer Metadata API",
                version = "v1alpha"))
public interface IssuerMetadataApi {

    @Tag(name = "Issuer Metadata API")
    @Operation(description = "Requests information about the capabilities of this issuer.",
            operationId = "getIssuerMetadata",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gets the issuer metadata.",
                            content = @Content(schema = @Schema(implementation = ApiSchema.IssuerMetadataSchema.class)))
            }
    )
    JsonObject getIssuerMetadata(String participantContextId, String token);
}
