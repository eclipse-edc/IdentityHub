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

package org.eclipse.edc.identithub.verifiablepresentation.generators;

public interface TestData {
    String LDP_VC_WITH_PROOF = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "id": "http://example.gov/credentials/3732",
              "type": ["VerifiableCredential", "UniversityDegreeCredential"],
              "issuer": "https://example.edu",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "BachelorDegree",
                  "name": "Bachelor of Science and Arts"
                }
              },
              "proof": {
                "type": "Ed25519Signature2020",
                "created": "2021-11-13T18:19:39Z",
                "verificationMethod": "https://example.edu/issuers/14#key-1",
                "proofPurpose": "assertionMethod",
                "proofValue": "z58DAdFfa9SkqZMVPxAQpic7ndSayn1PzZs6ZjWp1CktyGesjuTSwRdoWhAfGFCF5bppETSTojQCrfFPP2oumHKtz"
              }
            }
            """;

    String JWT_VC =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOmFiZmUxM2Y3MTIxMjA0MzFjMjc2ZTEyZWNhYiNrZXlzLTEifQ" +
                    ".eyJzdWIiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJodHRwOi8vZXhhbXBsZS5lZHUvY" +
                    "3JlZGVudGlhbHMvMzczMiIsImlzcyI6Imh0dHBzOi8vZXhhbXBsZS5jb20va2V5cy9mb28uandrIiwibmJmIjoxNTQxNDkzNzI0LCJ" +
                    "pYXQiOjE1NDE0OTM3MjQsImV4cCI6MTU3MzAyOTcyMywibm9uY2UiOiI2NjAhNjM0NUZTZXIiLCJ2YyI6eyJAY29udGV4dCI6WyJod" +
                    "HRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V" +
                    "4YW1wbGVzL3YxIl0sInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJVbml2ZXJzaXR5RGVncmVlQ3JlZGVudGlhbCJdLCJjc" +
                    "mVkZW50aWFsU3ViamVjdCI6eyJkZWdyZWUiOnsidHlwZSI6IkJhY2hlbG9yRGVncmVlIiwibmFtZSI6IjxzcGFuIGxhbmc9J2ZyLUNB" +
                    "Jz5CYWNjYWxhdXLDqWF0IGVuIG11c2lxdWVzIG51bcOpcmlxdWVzPC9zcGFuPiJ9fX19.KLJo5GAyBND3LDTn9H7FQokEsUEi8jKwXh" +
                    "GvoN3JtRa51xrNDgXDb0cq1UTYB-rK4Ft9YVmR1NI_ZOF8oGc_7wAp8PHbF2HaWodQIoOBxxT-4WNqAxft7ET6lkH-4S6Ux3rSGAmc" +
                    "zMohEEf8eCeN-jC8WekdPl6zKZQj0YPB1rx6X0-xlFBs7cl6Wt8rfBP_tZ9YgVWrQmUWypSioc0MUyiphmyEbLZagTyPlUyflGlEdqr" +
                    "ZAv6eSe6RtxJy6M1-lD7a5HTzanYTWBPAUHDZGyGKXdJw-W_x0IWChBzI8t3kpG253fg6V3tPgHeKXE94fz_QpYfg--7kLsyBAfQGbg";

    String LDP_VP_WITH_PROOF = """
            {
              "id": "urn:uuid:3978344f-8596-4c3a-a978-8fcaba3903c5",
              "type": "VerifiablePresentation",
              "verifiableCredential": {
                "issuanceDate": "2023-06-12T13:13:30Z",
                "credentialSubject": {
                  "http://schema.org/identifier": "member0123456789",
                  "id": "did:web:localhost:member0123456789",
                  "type": "https://org.eclipse.edc/linkedCredentialData#MembershipCredential"
                },
                "id": "https://org.eclipse.edc/testcases/t0001",
                "type": [
                  "VerifiableCredential"
                ],
                "issuer": "did:web:localhost:member0123456789",
                "expirationDate": "2024-12-31T23:00:00Z",
                "proof": {
                  "type": "JsonWebSignature2020",
                  "created": "2022-12-31T23:00:00Z",
                  "proofPurpose": "assertionMethod",
                  "verificationMethod": "https://org.eclipse.edc/verification-method",
                  "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..SwEkR4duA97jHy_WSVKIHLJqd8i2IidedmlMpUKyeV0YlPNz0pjPEKM9p7PqBb7oRIKG3-5qCxpzNhbsIEZZMzEMjWE1adckJ9SMiNr_G1wiAh3Op0cZHDgZBevIPElG"
                }
              },
              "proof": {
                "type": "JsonWebSignature2020",
                "created": "2022-12-31T23:00:00Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "https://org.eclipse.edc/verification-method",
                "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..eadcijhno0JUZ2yl2QrlQBD_1rrGFS1qjYeGYV8O-XN1P-28HneLnHkvUH9IDTiTTAwnCQjdr0Tq3NEgpbz-sji0X9fT-chM86OQfqylm0Dt6_jLIj-32JHtetFU3QXS"
              },
              "@context": [
                "https://www.w3.org/ns/did/v1",
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/security/suites/jws-2020/v1"
              ]
            }
            """;
    String JWT_VP =
            "eyJhbGciOiJFZERTQSJ9.eyJuYmYiOjE2MDI3NjQ4MDEsImlzcyI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMS" +
                    "IsInZwIjp7IkBjb250ZXh0IjoiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJ0eXBlIjoiVmVyaWZpY" +
                    "WJsZVByZXNlbnRhdGlvbiIsInZlcmlmaWFibGVDcmVkZW50aWFsIjoiZXlKaGJHY2lPaUpGWkVSVFFTSjkuZXlKemRXSWlPaUpr" +
                    "YVdRNlpYaGhiWEJzWlRwbFltWmxZakZtTnpFeVpXSmpObVl4WXpJM05tVXhNbVZqTWpFaUxDSnVZbVlpT2pFMU5qQTNNVEUwTVR" +
                    "rc0ltbHpjeUk2SW1ScFpEcGxlR0Z0Y0d4bE9qYzJaVEV5WldNM01USmxZbU0yWmpGak1qSXhaV0ptWldJeFppSXNJbVY0Y0NJNk" +
                    "1UVTJNRGM1TnpneE9Td2lkbU1pT25zaVFHTnZiblJsZUhRaU9sc2lhSFIwY0hNNkx5OTNkM2N1ZHpNdWIzSm5Mekl3TVRndlkzS" +
                    "mxaR1Z1ZEdsaGJITXZkakVpTENKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdk1qQXhPQzlqY21Wa1pXNTBhV0ZzY3k5bGVHRnRj" +
                    "R3hsY3k5Mk1TSmRMQ0owZVhCbElqcGJJbFpsY21sbWFXRmliR1ZEY21Wa1pXNTBhV0ZzSWl3aVZXNXBkbVZ5YzJsMGVVUmxaM0p" +
                    "sWlVOeVpXUmxiblJwWVd3aVhTd2lZM0psWkdWdWRHbGhiRk4xWW1wbFkzUWlPbnNpWTI5c2JHVm5aU0k2SWxSbGMzUWdWVzVwZG" +
                    "1WeWMybDBlU0lzSW1SbFozSmxaU0k2ZXlKdVlXMWxJam9pUW1GamFHVnNiM0lnYjJZZ1UyTnBaVzVqWlNCaGJtUWdRWEowY3lJc" +
                    "0luUjVjR1VpT2lKQ1lXTm9aV3h2Y2tSbFozSmxaU0o5Zlgwc0ltcDBhU0k2SW1oMGRIQTZMeTlsZUdGdGNHeGxMbVZrZFM5amNt" +
                    "VmtaVzUwYVdGc2N5OHpOek15SW4wLkdEcENPbHhpWjJpc0JRbjE1MWk1UGoyZS1rVWdrTmdfd3p4Q1BBZnhMeHRkT3o0ZnBEaW1" +
                    "nODFtTnczTHNuTzBHNTZBT1R2RDRTdXpTUXlqMWNQM0JnIn0sImlhdCI6MTYwMjc2NDgwMSwianRpIjoidXJuOnV1aWQ6ZWM3ND" +
                    "E1NTYtM2Y2ZS00ODkxLWJlNTQtNzRjMjNmZDkzNjA1In0.kv4Votk1DpFT4Irr-v85W3lorPo9r2p9qwdDrq4kH_veo7qTKtiNh" +
                    "C7BshUwP7zDN5_gD3GTr68OoNks2LoXDw";

    String EMPTY_LDP_VP = """
            {
              "@id": "https://w3id.org/tractusx-trust/v0.8/id/5ce0eb84-c12d-413f-9d3f-c6a9ce339490",
              "@type": [
                "https://www.w3.org/2018/credentials#VerifiablePresentation",
                "SomeOtherPresentationType"
              ],
              "https://www.w3.org/2018/credentials#holder": [
                {
                  "@id": "did:web:test-issuer"
                }
              ],
              "https://www.w3.org/2018/credentials#verifiableCredential": [],
              "https://w3id.org/security#proof": [
                {
                  "@type": [
                    "https://w3id.org/security#JsonWebSignature2020"
                  ],
                  "https://w3id.org/security#jws": [
                    {
                      "@value": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..XDuivXfZ-NMeoYDtsQHTpX6vpWiJNYEZYiAtNX09eVGHCXznSqdudliPYARXwaI1WbgDaH8kvVho8a8Z5aZcCA"
                    }
                  ]
                }
              ]
            }
            """;

    String EMPTY_JWT_VP = "eyJraWQiOiJodHRwczovL3Rlc3QuY29tL3Rlc3Qta2V5cyNrZXktMSIsImFsZyI6IkVTMzg0In0.eyJhdWQiOiJkaWQ6d2V" +
            "iOnRlc3QtYXVkaWVuY2UiLCJuYmYiOjE2OTk5Nzg2NTksImlzcyI6ImRpZDp3ZWI6dGVzdC1pc3N1ZXIiLCJ2cCI6IntcIkBjb250ZXh0XC" +
            "I6W1wiaHR0cHM6Ly93M2lkLm9yZy90cmFjdHVzeC10cnVzdC92MC44XCIsXCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy" +
            "92MVwiLFwiaHR0cHM6Ly9pZGVudGl0eS5mb3VuZGF0aW9uL3ByZXNlbnRhdGlvbi1leGNoYW5nZS9zdWJtaXNzaW9uL3YxXCJdLFwidHlwZ" +
            "VwiOlwiVmVyaWZpYWJsZVByZXNlbnRhdGlvblwiLFwidmVyaWZpYWJsZUNyZWRlbnRpYWxcIjpbXX0iLCJleHAiOjE2OTk5Nzg3MjEsImlhd" +
            "CI6MTY5OTk3ODY1OSwianRpIjoiYTEzN2JkMDUtMjAxOS00Yjg3LTlhN2UtYzdlODBjOTNlNzFjIn0.TdeDOMpCCHOCHmVZjNOg0L4e2gFc" +
            "v6Pz_Adwg_SrGT0Cv94EzoGl9bl7LwcuK7mtTbbzLbOnwUpVk8xhDfiME8sVENjCMYJP9Vz1QaT32e6cGWdtAIgLHnZ7RelRd5DH";
}
