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

package org.eclipse.edc.identityhub.api.didmanagement.v1;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Collection;

@OpenAPIDefinition(
        info = @Info(description = "This is the Management API for DID documents", title = "DID Management API", version = "1"))
public interface DidManagementApi {

    @Tag(name = "DID Management API")
    @Operation(description = "Stores a new DID document and optionally also publishes it",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DidDocument.class), mediaType = "application/json")),
            parameters = {@Parameter(name = "publish", description = "Indicates whether the DID should be published right after creation")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully stored"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, for example the DID document was invalid",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "Can't create the DID document, because a document with the same ID already exists",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void createDidDocument(DidDocument document, boolean publish);

    @Tag(name = "DID Management API")
    @Operation(description = "Publish an (existing) DID document. The DID is expected to exist in the database.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DidRequestPayload.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully published."),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The DID could not be published because it does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void publishDidFromBody(DidRequestPayload didRequestPayload);

    @Tag(name = "DID Management API")
    @Operation(description = "Un-Publish an (existing) DID document. The DID is expected to exist in the database.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DidRequestPayload.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully un-published."),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The DID could not be unpublished because the underlying VDR does not support un-publishing.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The DID could not be un-published because it does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void unpublishDidFromBody(DidRequestPayload didRequestPayload);

    @Tag(name = "DID Management API")
    @Operation(description = "Updates an (existing) DID document and re-publishes it if so desired. The DID is expected to exist in the database.",
            parameters = {@Parameter(name = "republish", description = "Indicates whether the DID document should be re-published after the update.")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully updated."),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The DID could not be updated because it does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void updateDid(DidDocument document, boolean republish);

    @Tag(name = "DID Management API")
    @Operation(description = "Delete an (existing) DID document. The DID is expected to exist in the database.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DidRequestPayload.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully deleted."),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The DID could not be deleted because it does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "The DID could not be deleted because it is already published. Un-publish first.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json"))
            }
    )
    void deleteDidFromBody(DidRequestPayload request);

    @Tag(name = "DID Management API")
    @Operation(description = "Query for DID documents..",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = QuerySpec.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID document was successfully deleted.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DidDocument.class)))),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "The query was malformed or was not understood by the server.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    Collection<DidDocument> queryDid(QuerySpec querySpec);

    @Tag(name = "DID Management API")
    @Operation(description = "Get state of a DID document",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DidRequestPayload.class), mediaType = "application/json")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The DID state was successfully obtained"),
                    @ApiResponse(responseCode = "401", description = "The request could not be completed, because either the authentication was missing or was not valid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "The DID does not exist.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)), mediaType = "application/json")),
            }
    )
    String getState(DidRequestPayload request);
}
