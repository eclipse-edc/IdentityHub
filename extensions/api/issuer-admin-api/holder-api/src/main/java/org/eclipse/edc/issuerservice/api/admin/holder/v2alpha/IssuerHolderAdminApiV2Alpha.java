/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.api.admin.holder.v2alpha;

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
import org.eclipse.edc.issuerservice.api.admin.holder.v1.IssuerHolderAdminApi;
import org.eclipse.edc.issuerservice.api.admin.holder.v2alpha.model.HolderPatchDto;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.web.spi.ApiErrorDetail;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate holders in an Issuer Service", title = "Issuer Service Holder Admin API", version = "2alpha"))
@Tag(name = "Issuer Service Holder Admin API v2alpha")
public interface IssuerHolderAdminApiV2Alpha extends IssuerHolderAdminApi {

    @Operation(description = "Updates the properties of a holder. 'did' and 'name' must be equal to those of the stored holder and cannot be changed. " +
            "The entries of 'properties' are added to the holder's properties, replacing the values of existing keys, and the keys in " +
            "'removed' are removed. Everything else is left as it is.",
            operationId = "patchHolder",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = HolderPatchDto.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The holder was updated successfully. The response contains the updated holder.",
                            content = @Content(schema = @Schema(implementation = Holder.class), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, or the DID or the name do not match the stored holder",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Can't update the holder because it was not found.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    Holder patchHolder(String participantContextId, String holderId, HolderPatchDto patch, SecurityContext context);
}
