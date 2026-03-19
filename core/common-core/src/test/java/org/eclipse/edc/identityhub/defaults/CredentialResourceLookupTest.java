/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.defaults;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialResourceLookupTest {

    private static final String VC_JSON = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1",
                "https://example.org/2026/foo/bar"
              ],
              "id": "http://example.edu/credentials/1872",
              "type": [
                "VerifiableCredential",
                "AlumniCredential"
              ],
              "issuer": "https://example.edu/issuers/565049",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "expirationDate": "2999-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "alumniOf": {
                  "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                  "name": "Example University"
                }
              },
              "proof": {
                "type": "RsaSignature2018",
                "created": "2017-06-18T21:19:10Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "https://example.edu/issuers/565049#key-1",
                "jws": "eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X"
              }
            }
            """;

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private CredentialResourceLookup lookup;

    @BeforeEach
    void setUp() {
        lookup = new CredentialResourceLookup();
    }

    @Test
    void getProperty_whenContextFieldRequested_shouldReplaceAtSymbol() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.@context", resource);

        assertThat(result).isNotNull();
    }

    @Test
    void getProperty_whenInstantField_shouldReturnString() {
        var timestamp = Instant.parse("2024-01-01T10:00:00Z");
        var resource = VerifiableCredentialResource.Builder.newHolder()
                .id("test-id")
                .issuerId("test-issuer")
                .holderId("test-holder")
                .build();
        resource.setCredentialStatus(VcStatus.INITIAL);

        var result = lookup.getProperty("timeOfLastStatusUpdate", resource);

        assertThat(result).isInstanceOf(String.class);
    }

    @Test
    void getProperty_whenRawVcField_shouldRemoveNewlines() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.rawVc", resource);

        assertThat(result).isInstanceOf(String.class);
        assertThat((String) result).doesNotContain("\n");
    }

    @Test
    void getProperty_whenCredentialSubjectClaim_shouldExtractFromClaims() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.credentialSubject.alumniOf", resource);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        var alumniOf = (Map<String, Object>) result;
        assertThat(alumniOf).containsEntry("name", "Example University");
    }

    @Test
    void getProperty_whenCredentialSubjectNestedClaim_shouldExtractNestedValue() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.credentialSubject.alumniOf.name", resource);

        assertThat(result).isEqualTo("Example University");
    }

    @Test
    void getProperty_whenCredentialSubjectClaimNotFound_shouldReturnNull() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.credentialSubject.nonExistentField", resource);

        assertThat(result).isNull();
    }

    @Test
    void getProperty_whenCredentialSubjectId_shouldExtractId() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.credentialSubject.id", resource);

        assertThat(result).isInstanceOf(List.class);
        assertThat((List<String>) result).contains("did:example:ebfeb1f712ebc6f1c276e12ec21");
    }

    @Test
    void getProperty_whenStandardField_shouldReturnValue() {
        var resource = VerifiableCredentialResource.Builder.newHolder()
                .id("test-resource-id")
                .issuerId("test-issuer")
                .holderId("test-holder")
                .build();

        var result = lookup.getProperty("id", resource);

        assertThat(result).isEqualTo("test-resource-id");
    }

    @Test
    void getProperty_whenNestedCredentialField_shouldReturnValue() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.issuer", resource);

        assertThat(result).isNotNull();
    }

    @Test
    void getProperty_whenNonExistentField_shouldReturnNull() {
        var resource = createTestResource();

        var result = lookup.getProperty("nonExistentField", resource);

        assertThat(result).isNull();
    }

    @Test
    void getProperty_whenCredentialType_shouldReturnTypeList() {
        var resource = createTestResource();

        var result = lookup.getProperty("verifiableCredential.credential.type", resource);

        assertThat(result).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        var types = (List<String>) result;
        assertThat(types).containsExactlyInAnyOrder("VerifiableCredential", "AlumniCredential");
    }

    private VerifiableCredentialResource createTestResource() {
        try {
            var credential = objectMapper.readValue(VC_JSON, VerifiableCredential.class);
            var container = new VerifiableCredentialContainer(VC_JSON, CredentialFormat.VC1_0_LD, credential);

            return VerifiableCredentialResource.Builder.newHolder()
                    .id("test-id")
                    .credential(container)
                    .state(VcStatus.INITIAL)
                    .issuerId("test-issuer")
                    .holderId("test-holder")
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}