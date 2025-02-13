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
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.tests;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubCustomizableEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.identityhub.tests.fixtures.PostgresSqlService;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.REQUEST_ID_TERM;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.JWT_VC_EXAMPLE;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.VC_EXAMPLE_2;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageApiEndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
        protected static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
        private static final String TEST_PARTICIPANT_CONTEXT_ID = "consumer";
        private static final String TEST_PARTICIPANT_CONTEXT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(TEST_PARTICIPANT_CONTEXT_ID.getBytes());


        @BeforeEach
        void setup(IdentityHubEndToEndTestContext context) {
            createParticipant(context);
        }

        @AfterEach
        void teardown(ParticipantContextService contextService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, CredentialStore store, StsAccountStore accountStore) {
            // purge all participant contexts

            contextService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> contextService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            // purge all VCs
            store.query(QuerySpec.none())
                    .map(creds -> creds.stream().map(cred -> store.deleteById(cred.getId())).toList())
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
        }

        @Test
        void storeCredential(IdentityHubEndToEndTestContext context, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage(createCredentialContainer());
            context.getStorageEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1alpha/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/credentials")
                    .then()
                    .statusCode(200);

            assertThat(credentialStore.query(QuerySpec.max()).getContent())
                    .hasSize(1)
                    .allSatisfy(vc -> assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED));
        }

        @Test
        void storeCredential_didNotResolved(IdentityHubEndToEndTestContext context, CredentialStore credentialStore) {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.failure("not found"));
            var credentialMessage = createCredentialMessage(createCredentialContainer());
            context.getStorageEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1alpha/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/credentials")
                    .then()
                    .statusCode(401)
                    .body(Matchers.containsString("not found"));

        }

        @Test
        void storeCredential_tokenSignedWithWrongKey(IdentityHubEndToEndTestContext context, CredentialStore credentialStore) throws JOSEException {
            var wrongKey = new ECKeyGenerator(Curve.P_256).generate();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(wrongKey.toPublicKey()));

            var credentialMessage = createCredentialMessage(createCredentialContainer());
            context.getStorageEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken(CONSUMER_DID, PROVIDER_DID))
                    .body(credentialMessage)
                    .post("/v1alpha/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/credentials")
                    .then()
                    .statusCode(401)
                    .body(Matchers.containsString("Token verification failed"));
        }

        @Test
        void storeCredential_wrongCredentialFormat(IdentityHubEndToEndTestContext context, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", JWT_VC_EXAMPLE)
                    .add("format", "illegalFormat")
                    .build();

            var credentialMessage = createCredentialMessage(credentialContainer);
            context.getStorageEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1alpha/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/credentials")
                    .then()
                    .statusCode(400)
                    .body(Matchers.containsString("Invalid format"));
        }

        @Test
        void storeCredential_jsonLdCredential(IdentityHubEndToEndTestContext context, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", VC_EXAMPLE_2)
                    .add("format", CredentialFormat.VC1_0_LD.toString())
                    .build();

            var credentialMessage = createCredentialMessage(credentialContainer);
            context.getStorageEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1alpha/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/credentials")
                    .then()
                    .statusCode(200);

            assertThat(credentialStore.query(QuerySpec.max()).getContent())
                    .hasSize(1)
                    .allSatisfy(vc -> assertThat(vc.getVerifiableCredential().format()).isEqualTo(CredentialFormat.VC1_0_LD));
        }

        private void createParticipant(IdentityHubEndToEndTestContext context) {
            createParticipant(context, TEST_PARTICIPANT_CONTEXT_ID, CONSUMER_KEY);
        }

        private void createParticipant(IdentityHubEndToEndTestContext context, String participantContextId, ECKey participantKey) {
            var service = context.getRuntime().getService(ParticipantContextService.class);
            var vault = context.getRuntime().getService(Vault.class);

            var privateKeyAlias = "%s-privatekey-alias".formatted(participantContextId);
            vault.storeSecret(privateKeyAlias, participantKey.toJSONString());
            var manifest = ParticipantManifest.Builder.newInstance()
                    .participantId(participantContextId)
                    .did("did:web:%s".formatted(participantContextId.replace("did:web:", "")))
                    .active(true)
                    .key(KeyDescriptor.Builder.newInstance()
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();
            service.createParticipantContext(manifest)
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        }

        private JsonObject createCredentialMessage(JsonObject... credentials) {
            var credentialContainers = Json.createArrayBuilder();

            Arrays.stream(credentials).forEach(credentialContainers::add);


            var credentialsJsonArray = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                            .add(JsonLdKeywords.VALUE, credentialContainers.build()));

            return Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(REQUEST_ID_TERM), "test-request-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), credentialsJsonArray)
                    .build();
        }

        private JsonObject createCredentialContainer() {
            return Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", JWT_VC_EXAMPLE)
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static IdentityHubCustomizableEndToEndExtension runtime;

        static {
            var ctx = IdentityHubEndToEndExtension.InMemory.context();
            ctx.getRuntime().registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
            ctx.getRuntime().registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
            runtime = new IdentityHubCustomizableEndToEndExtension(ctx);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        private static final String DB_NAME = "runtime";
        private static final Integer DB_PORT = getFreePort();

        @RegisterExtension
        @Order(1)
        static IdentityHubCustomizableEndToEndExtension runtime;
        static PostgresSqlService server = new PostgresSqlService(DB_NAME, DB_PORT);

        @Order(0) // must be the first extension to be evaluated since it starts the DB server
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            server.start();
        };

        static {
            var ctx = IdentityHubEndToEndExtension.Postgres.context(DB_NAME, DB_PORT);
            ctx.getRuntime().registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
            ctx.getRuntime().registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
            runtime = new IdentityHubCustomizableEndToEndExtension(ctx);
        }

    }
}
