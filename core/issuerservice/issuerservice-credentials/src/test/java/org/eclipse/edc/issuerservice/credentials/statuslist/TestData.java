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

package org.eclipse.edc.issuerservice.credentials.statuslist;

public class TestData {
    public static final String EXAMPLE_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2",
                "https://www.w3.org/ns/credentials/examples/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": [
                "VerifiableCredential",
                "ExampleDegreeCredential",
                "ExamplePersonCredential"
              ],
              "issuer": "https://university.example/issuers/14",
              "validFrom": "2010-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "ExampleBachelorDegree",
                  "name": "Bachelor of Science and Arts"
                },
                "alumniOf": {
                  "name": "Example University"
                }
              },
              "credentialSchema": [
                {
                  "id": "https://example.org/examples/degree.json",
                  "type": "JsonSchema"
                },
                {
                  "id": "https://example.org/examples/alumni.json",
                  "type": "JsonSchema"
                }
              ],
              "credentialStatus": [{
                "id": "https://example.com/credentials/status/3#94567",
                "type": "BitstringStatusListEntry",
                "statusPurpose": "revocation",
                "statusListIndex": "94567",
                "statusListCredential": "https://example.com/credentials/status/3"
              }]
            }
            """;

    /**
     * jwt representation of {@link TestData#EXAMPLE_CREDENTIAL}, signed with {@link TestData#SIGNING_KEY}
     */
    public static final String EXAMPLE_CREDENTIAL_JWT = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ.eyJAY29udGV4dCI6W
            yJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvZXhhbXBsZ
            XMvdjIiXSwiaWQiOiJodHRwOi8vdW5pdmVyc2l0eS5leGFtcGxlL2NyZWRlbnRpYWxzLzM3MzIiLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZ
            W50aWFsIiwiRXhhbXBsZURlZ3JlZUNyZWRlbnRpYWwiLCJFeGFtcGxlUGVyc29uQ3JlZGVudGlhbCJdLCJpc3N1ZXIiOiJodHRwczovL3Vua
            XZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzE0IiwidmFsaWRGcm9tIjoiMjAxMC0wMS0wMVQxOToyMzoyNFoiLCJjcmVkZW50aWFsU3ViamVjd
            CI6eyJpZCI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsImRlZ3JlZSI6eyJ0eXBlIjoiRXhhbXBsZUJhY2hlb
            G9yRGVncmVlIiwibmFtZSI6IkJhY2hlbG9yIG9mIFNjaWVuY2UgYW5kIEFydHMifSwiYWx1bW5pT2YiOnsibmFtZSI6IkV4YW1wbGUgVW5pd
            mVyc2l0eSJ9fSwiY3JlZGVudGlhbFNjaGVtYSI6W3siaWQiOiJodHRwczovL2V4YW1wbGUub3JnL2V4YW1wbGVzL2RlZ3JlZS5qc29uIiwid
            HlwZSI6Ikpzb25TY2hlbWEifSx7ImlkIjoiaHR0cHM6Ly9leGFtcGxlLm9yZy9leGFtcGxlcy9hbHVtbmkuanNvbiIsInR5cGUiOiJKc29uU
            2NoZW1hIn1dLCJjcmVkZW50aWFsU3RhdHVzIjpbeyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5jb20vY3JlZGVudGlhbHMvc3RhdHVzLzMjOTQ1N
            jciLCJ0eXBlIjoiQml0c3RyaW5nU3RhdHVzTGlzdEVudHJ5Iiwic3RhdHVzUHVycG9zZSI6InJldm9jYXRpb24iLCJzdGF0dXNMaXN0SW5kZ
            XgiOiI5NDU2NyIsInN0YXR1c0xpc3RDcmVkZW50aWFsIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9jcmVkZW50aWFscy9zdGF0dXMvMyJ9XX0.w
            xGckEQrJ1UEoZXIHbzREAU3FbTHosAdLJ4isERdmi-5OARS6wH5HTpdQ_ADPuGpzqJ5ci3gvxLI_UzJDJaemw
            """;

    /**
     * the key that was used to generate {@link TestData#EXAMPLE_CREDENTIAL_JWT} out of {@link TestData#EXAMPLE_CREDENTIAL}
     */
    public static final String SIGNING_KEY = """
            {
              "kty": "EC",
              "d": "SbKv_rIJJUI-8Whx5Zo1O20V-rOyKKQTKPpNY0UxtAY",
              "use": "sig",
              "crv": "P-256",
              "kid": "key-1",
              "x": "rKuOAlVttxmkLHz9NzxsR7Xj7xbzy2CcXfupHoA5VzA",
              "y": "fBP7UiEd05cnGDqoOKnOYwSSfuifJybtwyg7tbYfdiM"
            }
            """;

    public static final String EXAMPLE_REVOCATION_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2"
              ],
              "id": "https://example.com/credentials/status/3",
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
            """;
    public static final String EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2"
              ],
              "id": "https://example.com/credentials/status/3",
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
            """;

    /**
     * JWT representation of the revocation credential ({@link TestData#EXAMPLE_REVOCATION_CREDENTIAL}), signed with {@link TestData#SIGNING_KEY}
     */
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

    /**
     * JWT representation of the revocation credential ({@link TestData#EXAMPLE_REVOCATION_CREDENTIAL}), signed with {@link TestData#SIGNING_KEY}
     * but with the revocation bit at index 94567 set to "true"
     */
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
}
