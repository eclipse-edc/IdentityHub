/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.keypair.v1.unstable;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;


@OpenAPIDefinition(info = @Info(description = "This is the Identity API for manipulating KeyPairResources", title = "KeyPairResources Identity API", version = "1"))
@Tag(name = "Key Pairs")
public interface GetAllKeyPairsApi {


    @Operation(description = "Get all KeyPair resources across all Participant Contexts. Requires elevated access.",
            operationId = "getAllKeyPairs",
            parameters = {
                    @Parameter(name = "offset", description = "the paging offset. defaults to 0"),
                    @Parameter(name = "limit", description = "the page size. defaults to 50") },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of KeyPair resources.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KeyPairResource.class)))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<KeyPairResource> getAllKeyPairs(Integer offset, Integer limit);
}
