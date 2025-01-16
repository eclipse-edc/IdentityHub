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
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

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

    String VCDM20_JWT_VP = """
            eyJraWQiOiJ2cC1zaWduIiwiYWxnIjoiRVMyNTYifQ.eyJpZCI6ImRhdGE6YXBwbGljYXRpb24vdn
            Arand0LGV5SnJhV1FpT2lKMmNDMXphV2R1SWl3aVlXeG5Jam9pUlZNeU5UWWlmUS5leUowZVhCbEl
            qb2lWbVZ5YVdacFlXSnNaVkJ5WlhObGJuUmhkR2x2YmlJc0lrQmpiMjUwWlhoMElqcGJJbWgwZEhC
            ek9pOHZkM2QzTG5jekxtOXlaeTl1Y3k5amNtVmtaVzUwYVdGc2N5OTJNaUlzSW1oMGRIQnpPaTh2Z
            DNkM0xuY3pMbTl5Wnk5dWN5OWpjbVZrWlc1MGFXRnNjeTlsZUdGdGNHeGxjeTkyTWlKZExDSjJaWE
            pwWm1saFlteGxRM0psWkdWdWRHbGhiQ0k2VzNzaVFHTnZiblJsZUhRaU9pSm9kSFJ3Y3pvdkwzZDN
            keTUzTXk1dmNtY3Zibk12WTNKbFpHVnVkR2xoYkhNdmRqSWlMQ0pwWkNJNkltUmhkR0U2WVhCd2JH
            bGpZWFJwYjI0dmRtTXJhbmQwTEdWNVNuSmhWMUZwVDJsS01sbDVNWHBoVjJSMVNXbDNhVmxYZUc1S
            mFtOXBVbFpOZVU1VVdXbG1VUzVsZVVwcVkyMVdhMXBYTlRCaFYwWnpWVEpPYjFwWE1XaEphbkJpWl
            hsS2NGcERTVFpKYldnd1pFaENlazlwT0haYVdHaG9ZbGhDYzFwVE5YWmpiV04yV2xob2FHSllRbk5
            hV0UxMldrZFdibU50Vm14TWJYQjZZakkwYVV4RFNqQmxXRUpzU1dwdmFWTnVUblppYkU1cVlVZFdk
            RmxUU2psTVNITnBZVmRSYVU5cFNtOWtTRkozWTNwdmRrd3lWalJaVnpGM1lrZFZkV0l6U201TU1sW
            TBXVmN4ZDJKSFZucE1Na1p6WkZjeGRXRlROWEZqTWpsMVNXbDNhV1JJYkhkYVUwazJTV3R3ZW1JeU
            5WUlpNbWhzWWxkRmFXWldNSE5KYlU1NVdsZFNiR0p1VW5CWlYzaFVaRmRLY1ZwWFRqQkphbkEzU1c
            xc2EwbHFiMmxhUjJ4clQyMVdORmxYTVhkaVIxVTJXbGRLYlZwWFNYaGFhbU40VFcxV2FWbDZXbTFO
            VjAxNVRucGFiRTFVU214WmVrbDRTV2wzYVZwSFZtNWpiVlpzU1dwd04wbHVValZqUjFWcFQybEtSb
            VZIUm5SalIzaHNVVzFHYW1GSFZuTmlNMHBGV2xka2VWcFhWV2xNUTBwMVdWY3hiRWxxYjJsUmJVWn
            FZVWRXYzJJelNXZGlNbGxuVlRKT2NGcFhOV3BhVTBKb1ltMVJaMUZZU2pCamVVbzVURU5LYUdKSVZ
            uUmliV3hRV21sSk5tVjVTblZaVnpGc1NXcHZhVkpZYUdoaVdFSnpXbE5DVm1KdGJESmFXRXA2WVZo
            U05VbHVNVGxNUTBwd1drTkpOa2x0YURCa1NFRTJUSGs1TVdKdGJESmFXRXA2WVZoU05VeHRWalJaV
            npGM1lrZFZkbGt6U214YVIxWjFaRWRzYUdKSVRYWk5lbU42VFdsSmMwbHVXbWhpUjJ4clVtNUtkbU
            pUU1RaSmFrbDNUVlJCZEUxRVJYUk5SRVpWVFZSck5rMXFUVFpOYWxKaFNXbDNhV1JJYkhkYVUwazJ
            WM2xLVjFwWVNuQmFiV3hvV1cxNGJGRXpTbXhhUjFaMVpFZHNhR0pEU1hOSmExWTBXVmN4ZDJKSFZr
            VmFWMlI1V2xkV1JHTnRWbXRhVnpVd1lWZEdjMGxwZDJsU1dHaG9ZbGhDYzFwV1FteGpiazUyWW10T
            2VWcFhVbXhpYmxKd1dWZDNhVmhUZDJsUlIwNTJZbTVTYkdWSVVXbFBiSE5wWVVoU01HTklUVFpNZV
            RrelpETmpkV1I2VFhWaU0wcHVUREkxZWt3eVRubGFWMUpzWW01U2NGbFhlSHBNTTFsNVNXbDNhV0Z
            JVWpCalNFMDJUSGs1TTJRelkzVmtlazExWWpOS2Jrd3lOWHBNTWs1NVdsZFNiR0p1VW5CWlYzaDZU
            REpXTkZsWE1YZGlSMVo2VEROWmVVbHNNSE5KYld4Nll6TldiR05wU1RaSmJXZ3daRWhDZWs5cE9IW
            mtWelZ3WkcxV2VXTXliREJsVXpWc1pVZEdkR05IZUd4TU1teDZZek5XYkdOdVRYWk5WRkZwWmxFdV
            oxTnlWVTQxYlU4d2Eyd3hRbUZUZDFwNFJHSnZYMUJJVGtjeGQyMW1NVzExZERKMFZFaENUbGxKV21
            WWFVHSk9YM3BmU0MwNVpHdFBNbVJtWTFBNU0xZEtkM2xGTldWdlNqVnJjVjlxVEhwVFltOVdlbWNp
            TENKMGVYQmxJam9pUlc1MlpXeHZjR1ZrVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aWZTeDdJa
            0JqYjI1MFpYaDBJam9pYUhSMGNITTZMeTkzZDNjdWR6TXViM0puTDI1ekwyTnlaV1JsYm5ScFlXeH
            pMM1l5SWl3aWFXUWlPaUprWVhSaE9tRndjR3hwWTJGMGFXOXVMM1pqSzJwM2RDeGxlVXB5WVZkUmF
            VOXBTakpaZVRGNllWZGtkVWxwZDJsWlYzaHVTV3B2YVZKV1RYbE9WRmxwWmxFdVpYbEthbU50Vm10
            YVZ6VXdZVmRHYzFVeVRtOWFWekZvU1dwd1ltVjVTbkJhUTBrMlNXMW9NR1JJUW5wUGFUaDJXbGhvY
            UdKWVFuTmFVelYyWTIxamRscFlhR2hpV0VKeldsaE5kbHBIVm01amJWWnNURzF3ZW1JeU5HbE1RMG
            93WlZoQ2JFbHFiMmxUYms1MllteE9hbUZIVm5SWlUwbzVURWh6YVdGWFVXbFBhVXB2WkVoU2QyTjZ
            iM1pNTWxZMFdWY3hkMkpIVlhWaU0wcHVUREpXTkZsWE1YZGlSMVo2VERKR2MyUlhNWFZoVXpWeFl6
            STVkVWxwZDJsa1NHeDNXbE5KTmtscmNIcGlNalZVV1RKb2JHSlhSV2xtVmpCelNXMU9lVnBYVW14a
            WJsSndXVmQ0VkdSWFNuRmFWMDR3U1dwd04wbHRiR3RKYW05cFdrZHNhMDl0VmpSWlZ6RjNZa2RWTm
            xwWFNtMWFWMGw0V21wamVFMXRWbWxaZWxwdFRWZE5lVTU2V214TlZFcHNXWHBKZUVscGQybGFSMVp
            1WTIxV2JFbHFjRGRKYmxJMVkwZFZhVTlwU2tabFIwWjBZMGQ0YkZGdFJtcGhSMVp6WWpOS1JWcFha
            SGxhVjFWcFRFTktkVmxYTVd4SmFtOXBVVzFHYW1GSFZuTmlNMGxuWWpKWloxVXlUbkJhVnpWcVdsT
            kNhR0p0VVdkUldFb3dZM2xLT1V4RFNtaGlTRlowWW0xc1VGcHBTVFpsZVVwMVdWY3hiRWxxYjJsU1
            dHaG9ZbGhDYzFwVFFsWmliV3d5V2xoS2VtRllValZKYmpFNVRFTktjRnBEU1RaSmJXZ3daRWhCTmt
            4NU9URmliV3d5V2xoS2VtRllValZNYlZZMFdWY3hkMkpIVlhaWk0wcHNXa2RXZFdSSGJHaGlTRTEy
            VFhwamVrMXBTWE5KYmxwb1lrZHNhMUp1U25aaVUwazJTV3BKZDAxVVFYUk5SRVYwVFVSR1ZVMVVhe
            lpOYWswMlRXcFNZVWxwZDJsa1NHeDNXbE5KTmxkNVNsZGFXRXB3V20xc2FGbHRlR3hSTTBwc1drZF
            dkV1JIYkdoaVEwbHpTV3RXTkZsWE1YZGlSMVpGV2xka2VWcFhWa1JqYlZacldsYzFNR0ZYUm5OSmF
            YZHBVbGhvYUdKWVFuTmFWa0pzWTI1T2RtSnJUbmxhVjFKc1ltNVNjRmxYZDJsWVUzZHBVVWRPZG1K
            dVVteGxTRkZwVDJ4emFXRklVakJqU0UwMlRIazVNMlF6WTNWa2VrMTFZak5LYmt3eU5YcE1NazU1V
            2xkU2JHSnVVbkJaVjNoNlRETlplVWxwZDJsaFNGSXdZMGhOTmt4NU9UTmtNMk4xWkhwTmRXSXpTbT
            VNTWpWNlRESk9lVnBYVW14aWJsSndXVmQ0ZWt3eVZqUlpWekYzWWtkV2Vrd3pXWGxKYkRCelNXMXN
            lbU16Vm14amFVazJTVzFvTUdSSVFucFBhVGgyWkZjMWNHUnRWbmxqTW13d1pWTTFiR1ZIUm5SalIz
            aHNUREpzZW1NelZteGpiazEyVFZSUmFXWlJMbWRUY2xWT05XMVBNR3RzTVVKaFUzZGFlRVJpYjE5U
            VNFNUhNWGR0WmpGdGRYUXlkRlJJUWs1WlNWcGxWMUJpVGw5NlgwZ3RPV1JyVHpKa1ptTlFPVE5YU2
            5kNVJUVmxiMG8xYTNGZmFreDZVMkp2Vm5wbklpd2lkSGx3WlNJNklrVnVkbVZzYjNCbFpGWmxjbWx
            tYVdGaWJHVkRjbVZrWlc1MGFXRnNJbjFkZlEuWXBxYldPZ1A3aFBfLWd2YTlUYzl1N1ZSTFhnSjI1
            WWx5UjhaTVNrRU1oQVkzQmJoclFDeEVQT3YzaTEyc1dUUDd3U2gtN2Nhak92Q2JlTk8zTDhzRGciL
            CJ0eXBlIjoiRW52ZWxvcGVkVmVyaWZpYWJsZVByZXNlbnRhdGlvbiIsIkBjb250ZXh0IjpbImh0dH
            BzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy92MiIsImh0dHBzOi8vd3d3LnczLm9yZy9ucy9
            jcmVkZW50aWFscy9leGFtcGxlcy92MiJdfQ.5HZ1dfEachsqXkX9WvWjemyhsq6ZYztqz5H2BZOFW
            4zlR7RIfw50Piz83Q8lRqnH5ofBRLsXl4Dvk7k4mwJuUA
            """;

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

    String ENVELOPED_CREDENTIAL_JSON = """
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
              }
            }
            """;
}
