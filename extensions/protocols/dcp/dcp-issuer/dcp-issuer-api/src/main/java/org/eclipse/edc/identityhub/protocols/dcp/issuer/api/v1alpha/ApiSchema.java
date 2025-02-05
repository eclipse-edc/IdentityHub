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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;

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

    @Schema(name = "CredentialRequestMessage", example = CredentialRequestMessageSchema.RESPONSE_EXAMPLE)
    record CredentialRequestMessageSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(requiredMode = REQUIRED)
            List<CredentialRequestSchema> credentials
    ) {

        public static final String RESPONSE_EXAMPLE = """
                {
                   "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                   ],
                   "type": "CredentialRequestMessage",
                   "credentials": [
                     {
                       "credentialType": "MembershipCredential",
                       "format": "vcdm11_jwt"
                     },
                     {
                       "credentialType": "OrganizationCredential",
                       "format": "vcdm11_ld"
                     },
                     {
                       "credentialType": "Iso9001Credential",
                       "format": "vcdm20_jose"
                     }
                   ]
                 }
                """;
    }

    @Schema(name = "CredentialRequest", example = CredentialRequestSchema.EXAMPLE)
    record CredentialRequestSchema(
            @Schema(name = "credentialType", requiredMode = REQUIRED)
            String credentialType,
            @Schema(name = "format", requiredMode = REQUIRED)
            String format
    ) {
        public static final String EXAMPLE = """
                {
                       "credentialType": "MembershipCredential",
                       "format": "vcdm11_jwt"
                }
                """;
    }

    @Schema(name = "CredentialStatus", example = CredentialStatusSchema.RESPONSE_EXAMPLE)
    record CredentialStatusSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = "type", requiredMode = REQUIRED)
            String type,
            @Schema(name = "status", requiredMode = REQUIRED)
            String status
    ) {

        public static final String RESPONSE_EXAMPLE = """
                {
                   "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                   ],
                   "type": "CredentialStatus",
                   "requestId": "requestId",
                   "status": "RECEIVED"
                 }
                """;
    }

    @Schema(name = "IssuerMetadata", example = IssuerMetadataSchema.RESPONSE_EXAMPLE)
    record IssuerMetadataSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = "type", requiredMode = REQUIRED)
            String type,
            @Schema(name = "credentialIssuer", requiredMode = REQUIRED)
            String credentialIssuer,
            @Schema(name = "status", requiredMode = REQUIRED)
            String status
    ) {

        public static final String RESPONSE_EXAMPLE = """
                {
                    "@context": [
                      "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                    ],
                    "type": "IssuerMetadata",
                    "credentialIssuer": "did:web:issuer-url",
                    "credentialsSupported": [
                      {
                        "type": "CredentialObject",
                        "credentialType": "MembershipCredential",
                        "offerReason": "reissue",
                        "bindingMethods": [
                          "did:web"
                        ],
                        "profiles": [
                          "vc20-bssl/jwt", "vc10-sl2021/jwt", "..."
                        ],
                        "issuancePolicy": {
                          "id": "Scalable trust example",
                          "input_descriptors": [
                            {
                              "id": "pd-id",
                              "constraints": {
                                "fields": [
                                  {
                                    "path": [
                                      "$.vc.type"
                                    ],
                                    "filter": {
                                      "type": "string",
                                      "pattern": "^AttestationCredential$"
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                """;
    }

    @Schema(name = "CredentialObject", example = CredentialObjectSchema.EXAMPLE)
    record CredentialObjectSchema(
            @Schema(name = "credentialType", requiredMode = REQUIRED)
            String credentialType,
            @Schema(name = "format", requiredMode = REQUIRED)
            String format
    ) {
        public static final String EXAMPLE = """
                {
                       "credentialType": "MembershipCredential",
                       "format": "vcdm11_jwt"
                }
                """;
    }
}
