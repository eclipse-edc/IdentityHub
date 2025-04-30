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

package org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable.model.IssuanceProcessDto;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(info = @Info(description = "This API is used to manage issuance processes in an Issuer Service", title = "Issuer Service Issuance Process Admin API", version = "1"))
@Tag(name = "Issuer Service Issuance Process Admin API")
public interface IssuanceProcessAdminApi {


    @Operation(description = "Gets an issuance process by its ID.",
            operationId = "getIssuanceProcessById",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The issuance process was found.",
                            content = @Content(schema = @Schema(implementation = IssuanceProcessDto.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The issuance process was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    IssuanceProcessDto getIssuanceProcessById(String participantContextId, String issuanceProcessId, SecurityContext securityContext);

    @Operation(description = "Gets all issuance processes for a certain query.",
            operationId = "queryIssuanceProcesses",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of issuance processes.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = IssuanceProcessDto.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the request could not be processed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<IssuanceProcessDto> queryIssuanceProcesses(String participantContextId, QuerySpec querySpec, SecurityContext securityContext);


}
