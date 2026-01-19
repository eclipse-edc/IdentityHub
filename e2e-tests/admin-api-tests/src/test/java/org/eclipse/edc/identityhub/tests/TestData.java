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

package org.eclipse.edc.identityhub.tests;

public class TestData {

    public static String exampleRevocationCredential(String credentialId) {
        return """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2"
              ],
              "id": "%s",
              "type": ["VerifiableCredential", "BitstringStatusListCredential"],
              "issuer": "did:example:12345",
              "validFrom": "2021-04-05T14:27:40Z",
              "credentialSubject": {
                "id": "https://example.com/status/3#list",
                "type": "BitstringStatusList",
                "statusPurpose": "revocation",
                "encodedList": "uH4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
              }
            }
            """.formatted(credentialId);
    }

    public static String exampleRevocationCredentialWithStatusBitSet(String credentialId) {
        return """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2"
              ],
              "id": "%s",
              "type": ["VerifiableCredential", "BitstringStatusListCredential"],
              "issuer": "did:example:12345",
              "validFrom": "2021-04-05T14:27:40Z",
              "credentialSubject": {
                "id": "https://example.com/status/3#list",
                "type": "BitstringStatusList",
                "statusPurpose": "revocation",
                "encodedList": "H4sIAAAAAAAA/+3OMQ0AAAgDsOHfNBp2kZBWQRMAAAAAAAAAAAAAAL6Z6wAAAAAAtQVQdb5gAEAAAA=="
              }
            }
            """.formatted(credentialId);
    }

    public static final String EXAMPLE_REVOCATION_CREDENTIAL_JWT = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ.eyJAY29udGV4dCI6W
            yJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiXSwiaWQiOiJodHRwczovL2V4YW1wbGUuY29tL2NyZWRlbnRpYWxzL3N0Y
            XR1cy8zIiwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJpdHN0cmluZ1N0YXR1c0xpc3RDcmVkZW50aWFsIl0sImlzc3VlciI6I
            mRpZDpleGFtcGxlOjEyMzQ1IiwidmFsaWRGcm9tIjoiMjAyMS0wNC0wNVQxNDoyNzo0MFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6I
            mh0dHBzOi8vZXhhbXBsZS5jb20vc3RhdHVzLzMjbGlzdCIsInR5cGUiOiJCaXRzdHJpbmdTdGF0dXNMaXN0Iiwic3RhdHVzUHVycG9zZSI6I
            nJldm9jYXRpb24iLCJlbmNvZGVkTGlzdCI6InVINHNJQUFBQUFBQUFBLTNCTVFFQUFBRENvUFZQYlF3Zm9BQUFBQUFBQUFBQUFBQUFBQUFBQ
            UlDM0FZYlNWS3NBUUFBQSJ9fQ.aPe5YXaNH-itNYYI7jE6FW3ttN2NzS5e1eNvkYw6BqW185w20xYKXQlZ7ETayqJXIcA7Q5HiyeVdKqPwkl
            nyLQ
            """;

    public static final String EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ.eyJAY29udGV4dCI6W
            yJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiXSwiaWQiOiJodHRwczovL2V4YW1wbGUuY29tL2NyZWRlbnRpYWxzL3N0Y
            XR1cy8zIiwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJpdHN0cmluZ1N0YXR1c0xpc3RDcmVkZW50aWFsIl0sImlzc3VlciI6I
            mRpZDpleGFtcGxlOjEyMzQ1IiwidmFsaWRGcm9tIjoiMjAyMS0wNC0wNVQxNDoyNzo0MFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6I
            mh0dHBzOi8vZXhhbXBsZS5jb20vc3RhdHVzLzMjbGlzdCIsInR5cGUiOiJCaXRzdHJpbmdTdGF0dXNMaXN0Iiwic3RhdHVzUHVycG9zZSI6I
            nJldm9jYXRpb24iLCJlbmNvZGVkTGlzdCI6Ikg0c0lBQUFBQUFBQS8rM09NUTBBQUFnRHNPSGZOQnAya1pCV1FSTUFBQUFBQUFBQUFBQUFBT
            DZaNndBQUFBQUF0UVZRZGI1Z0FFQUFBQT09In19.EI-kWzpDykZxbvedDgEG0cOJRFfEDZHJtHlnGD6fbQEm13GcLGKBMVT_KJEmsdjBBhys
            Sh0KW-2S2mm3jS9w1w
            """;

    public static final String ISSUER_RUNTIME_NAME = "issuerservice";
}
