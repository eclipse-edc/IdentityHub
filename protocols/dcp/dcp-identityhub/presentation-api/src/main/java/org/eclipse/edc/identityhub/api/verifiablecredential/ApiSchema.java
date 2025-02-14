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

package org.eclipse.edc.identityhub.api.verifiablecredential;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.json.JsonObject;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public interface ApiSchema {
    @Schema(name = "ApiErrorDetail", example = ApiErrorDetailSchema.API_ERROR_EXAMPLE)
    record ApiErrorDetailSchema(
            String message,
            String type,
            String path,
            String invalidValue
    ) {
        public static final String API_ERROR_EXAMPLE = """
                {
                    "message": "error message",
                    "type": "ErrorType",
                    "path": "object.error.path",
                    "invalidValue": "this value is not valid"
                }
                """;
    }

    @Schema(name = "PresentationQueryMessage", example = PresentationQuerySchema.PRESENTATION_QUERY_EXAMPLE)
    record PresentationQuerySchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, requiredMode = REQUIRED)
            String type,
            @Schema(name = "scope", requiredMode = NOT_REQUIRED)
            List<String> scope,
            @Schema(name = "presentationDefinition", requiredMode = NOT_REQUIRED)
            PresentationDefinitionSchema presentationDefinitionSchema
    ) {

        public static final String PRESENTATION_QUERY_EXAMPLE = """
                {
                  "@context": [
                    "https://w3id.org/tractusx-trust/v0.8",
                    "https://identity.foundation/presentation-exchange/submission/v1"
                  ],
                  "@type": "PresentationQueryMessage",
                  "presentationDefinition": null,
                  "scope": [
                    "org.eclipse.edc.vc.type:SomeCredential_0.3.5:write",
                    "org.eclipse.edc.vc.type:SomeOtherCredential:read",
                    "org.eclipse.edc.vc.type:ThirdCredential:*"
                  ]
                }
                """;
    }

    @Schema(name = "PresentationResponseMessage", example = PresentationResponseSchema.RESPONSE_EXAMPLE)
    record PresentationResponseSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = "presentation", requiredMode = REQUIRED, anyOf = {String.class, JsonObject.class})
            List<Object> presentation
    ) {

        public static final String RESPONSE_EXAMPLE = """
                {
                  "@context": [
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationResponseMessage",
                  "presentation": ["dsJdh...UMetV"]
                }
                """;
    }

    @Schema(name = "PresentationDefinitionSchema", example = PresentationDefinitionSchema.EXAMPLE)
    record PresentationDefinitionSchema() {

        private static final String EXAMPLE = """
                {
                  "comment": "taken from https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-definition"
                  "presentationDefinition": {
                    "id": "first simple example",
                    "input_descriptors": [
                      {
                        "id": "A specific type of VC",
                        "name": "A specific type of VC",
                        "purpose": "We want a VC of this type",
                        "constraints": {
                          "fields": [
                            {
                              "path": [
                                "$.type"
                              ],
                              "filter": {
                                "type": "string",
                                "pattern": "<the type of VC e.g. degree certificate>"
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;
    }
}
