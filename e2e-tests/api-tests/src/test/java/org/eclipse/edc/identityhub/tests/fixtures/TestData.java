/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

public interface TestData {
    // taken from https://www.w3.org/TR/vc-data-model/#example-a-simple-example-of-a-verifiable-credential
    String VC_EXAMPLE = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "id": "http://example.edu/credentials/1872",
              "type": ["VerifiableCredential", "AlumniCredential"],
              "issuer": "https://example.edu/issuers/565049",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "expirationDate": "2999-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "alumniOf": {
                  "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                  "name": [{
                    "value": "Example University",
                    "lang": "en"
                  }, {
                    "value": "Exemple d'Université",
                    "lang": "fr"
                  }]
                }
              },
              "proof": {
                "type": "RsaSignature2018",
                "created": "2017-06-18T21:19:10Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "https://example.edu/issuers/565049#key-1",
                "jws": "eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5XsITJX1CxPCT8yAV-TVkIEq_PbChOMqsLfRoPsnsgw5WEuts01mq-pQy7UJiN5mgRxD-WUcX16dUEMGlv50aqzpqh4Qktb3rk-BuQy72IFLOqV0G_zS245-kronKb78cPN25DGlcTwLtjPAYuNzVBAh4vGHSrQyHUdBBPM"
              }
            }
            """;

    // this VC is
    String VC_EXAMPLE_2 = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "id": "http://example.edu/credentials/1872",
              "type": ["VerifiableCredential", "SuperSecretCredential"],
              "issuer": "https://example.edu/issuers/12345",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "super-secret-content": 42
              },
              "proof": {
                "type": "RsaSignature2018",
                "created": "2017-06-18T21:19:10Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "https://example.edu/issuers/565049#key-1",
                "jws": "eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5XsITJX1CxPCT8yAV-TVkIEq_PbChOMqsLfRoPsnsgw5WEuts01mq-pQy7UJiN5mgRxD-WUcX16dUEMGlv50aqzpqh4Qktb3rk-BuQy72IFLOqV0G_zS245-kronKb78cPN25DGlcTwLtjPAYuNzVBAh4vGHSrQyHUdBBPM"
              }
            }
            """;
}
