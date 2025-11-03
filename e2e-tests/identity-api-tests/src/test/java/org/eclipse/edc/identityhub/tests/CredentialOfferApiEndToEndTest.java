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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class CredentialOfferApiEndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
        protected static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
        private static final String TEST_PARTICIPANT_CONTEXT_ID = "consumer";
        private static final String TEST_PARTICIPANT_CONTEXT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(TEST_PARTICIPANT_CONTEXT_ID.getBytes());

        @BeforeEach
        void setup(IdentityHub identityHub) {
            createParticipant(identityHub);
            identityHub.storeHolderRequest(HolderCredentialRequest.Builder.newInstance()
                    .id("test-holder-id")
                    .issuerDid(PROVIDER_DID)
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .requestedCredential("test-id1", "ExamplePersonCredential", "VC1_0_JWT")
                    .requestedCredential("test-id2", "SuperSecretCredential", "VC1_0_LD")
                    .state(REQUESTED.code())
                    .participantContextId(PROVIDER_DID)
                    .build());
        }

        @AfterEach
        void teardown(ParticipantContextService contextService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, CredentialOfferStore store, StsAccountStore accountStore) {
            // purge all participant contexts

            contextService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> contextService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            // purge all VCs
            store.query(QuerySpec.none())
                    .forEach(cred -> store.deleteById(cred.getId()).orElseThrow(f -> new RuntimeException(f.getFailureDetail())));

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
        }

        @DisplayName("Store CredentialOffer - success")
        @Test
        void storeCredentialOffer(IdentityHub identityHub, CredentialOfferStore credentialOfferStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var offerMessage = createCredentialOfferMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(offerMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/offers")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            await().untilAsserted(() -> assertThat(credentialOfferStore.query(QuerySpec.max()))
                    .hasSize(1)
                    .allSatisfy(co -> assertThat(co.getStateAsEnum()).isEqualTo(CredentialOfferStatus.PROCESSED)));
        }

        @DisplayName("Issuer's DID not resolvable, expect HTTP 401")
        @Test
        void storeCredentialOffer_didNotResolved(IdentityHub identityHub) {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.failure("not found"));
            var credentialMessage = createCredentialOfferMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/offers")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("not found"));

        }

        @DisplayName("Issuer's auth token invalid, expect HTTP 401")
        @Test
        void storeCredentialOffer_tokenSignedWithWrongKey(IdentityHub identityHub) throws JOSEException {
            var wrongKey = new ECKeyGenerator(Curve.P_256).generate();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(wrongKey.toPublicKey()));

            var credentialMessage = createCredentialOfferMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken(CONSUMER_DID, PROVIDER_DID))
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/offers")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("Token verification failed"));
        }

        @DisplayName("CredentialMessage contains an illegal format, expect HTTP 400")
        @Test
        void storeCredentialOffer_wrongCredentialFormat(IdentityHub identityHub) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var credentialObject = Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "reissuance")
                    //missing: credentialType
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("did:web")))
                    .build();

            var credentialMessage = createCredentialOfferMessage(credentialObject);
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID_ENCODED + "/offers")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("Invalid format"));
        }

        private void createParticipant(IdentityHub identityHub) {
            createParticipant(identityHub, CONSUMER_KEY);
        }

        private void createParticipant(IdentityHub identityHub, ECKey participantKey) {
            var service = identityHub.getService(ParticipantContextService.class);
            var vault = identityHub.getService(Vault.class);

            var privateKeyAlias = "%s-privatekey-alias".formatted(Tests.TEST_PARTICIPANT_CONTEXT_ID);
            vault.storeSecret(privateKeyAlias, participantKey.toJSONString());
            var manifest = ParticipantManifest.Builder.newInstance()
                    .participantContextId(Tests.TEST_PARTICIPANT_CONTEXT_ID)
                    .did("did:web:%s".formatted(Tests.TEST_PARTICIPANT_CONTEXT_ID.replace("did:web:", "")))
                    .active(true)
                    .key(KeyDescriptor.Builder.newInstance()
                            .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();
            service.createParticipantContext(manifest)
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        }

        private JsonObject createCredentialOfferMessage(JsonObject... credentials) {
            var credentialsArray = Json.createArrayBuilder();
            Arrays.stream(credentials).forEach(credentialsArray::add);
            return Json.createObjectBuilder()
                    .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CredentialOfferMessage.CREDENTIALS_TERM), credentialsArray)
                    .build();

        }

        private JsonObject createCredentialContainer() {
            return Json.createObjectBuilder()
                    .add(JsonLdKeywords.ID, UUID.randomUUID().toString())
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("vc20-bssl/jwt")))
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "reissuance")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("did:web")))
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String DB_NAME = "runtime";
        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(DB_NAME);
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);


    }
}
