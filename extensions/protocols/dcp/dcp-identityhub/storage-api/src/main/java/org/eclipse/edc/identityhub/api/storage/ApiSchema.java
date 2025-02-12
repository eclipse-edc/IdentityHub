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

package org.eclipse.edc.identityhub.api.storage;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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

    @Schema(name = "CredentialMessage", example = CredentialMessageSchema.CREDENTIALMESSAGE_EXAMPLE)
    record CredentialMessageSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, requiredMode = REQUIRED)
            String type,
            @Schema(name = "credentials", requiredMode = REQUIRED)
            List<CredentialContainerSchema> credentials,
            @Schema(name = "requestId", requiredMode = REQUIRED)
            String requestId
    ) {

        public static final String CREDENTIALMESSAGE_EXAMPLE = """
                {
                  "@context": [
                    "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                  ],
                  "type": "CredentialMessage",
                  "credentials": [
                    {
                      "credentialType": "MembershipCredential",
                      "payload": "",
                      "format": "jwt"
                    },
                    {
                      "credentialType": "OrganizationCredential",
                      "payload": "",
                      "format": "json-ld"
                    }
                  ],
                  "requestId": "requestId"
                }
                """;
    }


    @Schema(name = "CredentialContainerSchema", example = CredentialContainerSchema.EXAMPLE)
    record CredentialContainerSchema() {

        private static final String EXAMPLE = """
                {
                    "credentialType": "MembershipCredential",
                    "payload": "",
                    "format": "jwt"
                }
                """;
    }
}
