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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET;

public class CredentialApiEndToEndTest {
    public static final String SIGNING_KEY_ALIAS = "signing-key";
    public static final int STATUS_LIST_INDEX = 94567;
    private static final String STATUS_LIST_CREDENTIAL_ID = "https://example.com/credentials/status/3";
    private final ObjectMapper objectMapper = new JacksonTypeManager().getMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private @NotNull VerifiableCredentialResource createCredential(String credentialId) {
        var cred = VerifiableCredential.Builder.newInstance()
                .issuanceDate(Instant.now())
                .id(credentialId)
                .type("VerifiableCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance().id(UUID.randomUUID().toString()).claim("foo", "bar").build())
                .credentialStatus(new CredentialStatus(credentialId + "#status", "BitstringStatusListEntry", Map.of(
                        "statusListIndex", STATUS_LIST_INDEX,
                        "statusPurpose", "revocation",
                        "statusListCredential", STATUS_LIST_CREDENTIAL_ID
                )))
                .issuer(new Issuer(UUID.randomUUID().toString()))
                .build();
        return VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .issuerId("issuer-id")
                .holderId("holder-id")
                .id(credentialId)
                .credential(new VerifiableCredentialContainer("JWT_STRING", CredentialFormat.VC1_0_JWT, cred))
                .build();
    }

    private VerifiableCredentialResource createRevocationCredential(String credentialJson, String credentialJwt) {
        try {
            var credential = objectMapper.readValue(credentialJson, VerifiableCredential.class);
            return VerifiableCredentialResource.Builder.newInstance()
                    .state(VcStatus.ISSUED)
                    .issuerId("issuer-id")
                    .holderId("holder-id")
                    .id(STATUS_LIST_CREDENTIAL_ID)
                    .credential(new VerifiableCredentialContainer(credentialJwt, CredentialFormat.VC1_0_JWT, credential))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    abstract class Tests {


        @BeforeEach
        void prepare(Vault vault) throws JOSEException {
            // put signing key in vault
            vault.storeSecret(SIGNING_KEY_ALIAS, new ECKeyGenerator(Curve.P_256).generate().toJSONString());
        }

        @AfterEach
        void teardown(CredentialStore credentialStore) {
            credentialStore.query(QuerySpec.max()).getContent()
                    .forEach(vcr -> credentialStore.deleteById(vcr.getId()));
        }

        @Test
        void revoke_whenNotYetRevoked(IssuerServiceEndToEndTestContext context, CredentialStore credentialStore) {

            // create revocation credential
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            // track the original bitstring
            var originalBitstring = res.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            context.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/credentials/test-cred/revoke")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // verify that the status list credential's bitstring has changed
            var updatedBitstring = credentialStore.findById(STATUS_LIST_CREDENTIAL_ID).getContent()
                    .getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            assertThat(updatedBitstring).isNotEqualTo(originalBitstring);
        }

        @Test
        void revoke_whenAlreadyRevoked(IssuerServiceEndToEndTestContext context, CredentialStore credentialStore) {

            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET);

            // track the original bitstring
            var originalBitstring = res.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            context.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/credentials/test-cred/revoke")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // verify that the status list credential's bitstring has NOT changed
            var updatedBitstring = credentialStore.findById(STATUS_LIST_CREDENTIAL_ID).getContent()
                    .getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            assertThat(updatedBitstring).isEqualTo(originalBitstring);
        }

        @Test
        void revoke_whenCredentialNotFound(IssuerServiceEndToEndTestContext context, CredentialStore credentialStore) {
            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET);

            // track the original bitstring
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            // missing: creation of the holder credential

            context.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/credentials/test-cred/revoke")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(Matchers.containsString("was not found"));
        }

        @Test
        void revoke_whenStatusListCredentialNotFound(IssuerServiceEndToEndTestContext context, CredentialStore credentialStore) {

            //missing: create status list credential

            credentialStore.create(createCredential("test-cred"));

            context.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/credentials/test-cred/revoke")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(Matchers.containsString("was not found"));
        }

        @Test
        void revoke_whenWrongStatusListType(IssuerServiceEndToEndTestContext context, CredentialStore credentialStore) {
            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            // track the original bitstring
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            // create credential with invalid status type
            var credential = createCredential("test-cred");
            var status = credential.getVerifiableCredential().credential().getCredentialStatus();
            status.clear();
            status.add(new CredentialStatus("test-cred#status", "InvalidStatusListType", Map.of(
                    "statusListIndex", STATUS_LIST_INDEX,
                    "statusPurpose", "revocation",
                    "statusListCredential", STATUS_LIST_CREDENTIAL_ID
            )));
            credentialStore.create(credential);

            context.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .post("/v1alpha/credentials/test-cred/revoke")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(Matchers.containsString("No StatusList implementation for type 'InvalidStatusListType' found."));

        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {

    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IssuerServiceEndToEndExtension.Postgres.class)
    class Postgres extends Tests {

    }

}
