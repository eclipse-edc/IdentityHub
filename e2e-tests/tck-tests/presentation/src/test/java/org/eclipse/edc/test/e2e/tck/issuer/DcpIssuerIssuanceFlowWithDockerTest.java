/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.tck.issuer;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.e2e.tck.TckTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.CREDENTIAL_SIGNING;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.TOKEN_SIGNING;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@TckTest
@Testcontainers
public class DcpIssuerIssuanceFlowWithDockerTest {

    // Custom Endpoints with host.docker.internal
    private static final Endpoints ENDPOINTS = Endpoints.Builder.newInstance()
            .endpoint("issueradmin", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/admin"))
            .endpoint("issuance", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/issuance"))
            .endpoint("sts", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/sts"))
            .endpoint("identity", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/identity"))
            .endpoint("did", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/"))
            .endpoint("statuslist", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/statuslist"))
            .build();

    @RegisterExtension
    public static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
            .name("issuerservice")
            .modules(DefaultRuntimes.Issuer.MODULES)
            .endpoints(ENDPOINTS)
            .configurationProvider(DefaultRuntimes.Issuer::config)
            .paramProvider(IssuerService.class, IssuerService::forContext)
            .modules(DefaultRuntimes.Issuer.MODULES)
            .modules(":e2e-tests:tck-tests:test-attestations")
            .build();
    private static final int CALLBACK_PORT = getFreePort();
    private static final String ISSUANCE_CORRELATION_ID = "issuance-correlation-id";
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "issuer";
    public String issuerDid;
    private ECKey issuerKey;

    @BeforeEach
    void setup(IssuerService issuer) {
        issuerDid = issuer.didFor(TEST_PARTICIPANT_CONTEXT_ID);
        issuerKey = generateEcKey(issuerDid + "#key1");
    }

    @DisplayName("Run TCK Issuance tests targeting the Issuer (using Docker)")
    @Test
    void runIssuanceFlowTests(IssuerService issuer) throws InterruptedException {
        var monitor = new org.eclipse.edc.spi.monitor.ConsoleMonitor("TCK", ConsoleMonitor.Level.DEBUG, true);

        var issuancePort = issuer.getIssuerApiEndpoint().getUrl().getPort();
        var issuancePath = issuer.getIssuerApiEndpoint().getUrl().getPath();
        var stsPort = issuer.getStsEndpoint().getUrl().getPort();
        var stsPath = issuer.getStsEndpoint().getUrl().getPath();

        var holderDid = "did:web:0.0.0.0%%3A%s:holder".formatted(CALLBACK_PORT);

        var baseCallbackAddress = "http://0.0.0.0:%s".formatted(CALLBACK_PORT);
        var baseIssuerServiceUrl = "http://host.docker.internal:%s%s/v1alpha/participants/%s".formatted(issuancePort, issuancePath, Base64.encode(TEST_PARTICIPANT_CONTEXT_ID));
        var baseCallbackUri = URI.create(baseCallbackAddress);

        // prepare the issuer service:
        createHolder(issuer, holderDid);
        var response = createParticipantContext(issuer, baseIssuerServiceUrl);
        createDefinitions(issuer);

        try (var tckContainer = new GenericContainer<>("eclipsedataspacetck/dcp-tck-runtime:1.0.0-RC3")
                .withExtraHost("host.docker.internal", "host-gateway")
                .withExposedPorts(CALLBACK_PORT)
                .withEnv(Map.of(
                        "dataspacetck.callback.address", baseCallbackAddress,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.issuer", issuerDid,
                        "dataspacetck.sts.url", "http://host.docker.internal:%s%s".formatted(stsPort, stsPath),
                        "dataspacetck.sts.client.id", response.clientId(),
                        "dataspacetck.sts.client.secret", response.clientSecret(),
                        "dataspacetck.credentials.correlation.id", ISSUANCE_CORRELATION_ID,
                        "dataspacetck.test.package", "org.eclipse.dataspacetck.dcp.verification.issuance.issuer"
                ))
        ) {
            tckContainer.setPortBindings(List.of("%s:%s".formatted(CALLBACK_PORT, CALLBACK_PORT)));
            tckContainer.start();
            var latch = new CountDownLatch(1);
            var hasFailed = new AtomicBoolean(false);
            tckContainer.followOutput(outputFrame -> {
                monitor.info(outputFrame.getUtf8String());
                if (outputFrame.getUtf8String().toLowerCase().contains("there were failing tests")) {
                    hasFailed.set(true);
                }
                if (outputFrame.getUtf8String().toLowerCase().contains("test run complete")) {
                    latch.countDown();
                }

            });

            assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
            assertThat(hasFailed.get()).describedAs("There were failing TCK tests, please check the log output above").isFalse();
        }
    }

    private void createDefinitions(IssuerService issuer) {
        var attestationDefinitionService = issuer.getService(AttestationDefinitionService.class);
        attestationDefinitionService.createAttestation(AttestationDefinition.Builder.newInstance()
                        .id("tck-test-attestation")
                        .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                        .attestationType("tck-test").build())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var credentialDefinitionService = issuer.getService(CredentialDefinitionService.class);
        var count = new AtomicInteger(1);

        Stream.of("MembershipCredential", "SensitiveDataCredential").forEach(type -> {
            credentialDefinitionService.createCredentialDefinition(CredentialDefinition.Builder.newInstance()
                            .credentialType(type)
                            .id("credential-object-id%d".formatted(count.getAndIncrement()))
                            .attestation("tck-test-attestation")
                            .formatFrom(CredentialFormat.VC1_0_JWT)
                            .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                            .jsonSchemaUrl("https://example.com/schema/%s-schema.json".formatted(type.toLowerCase()))
                            .jsonSchema("{}")
                            .mapping(new MappingDefinition("participant.name", "credentialSubject.participant_name", true))
                            .build())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        });
    }

    private void createHolder(IssuerService issuer, String holderDid) {
        var holderStore = issuer.getService(HolderStore.class);
        holderStore.create(Holder.Builder.newInstance()
                .holderId(holderDid)
                .participantContextId(holderDid)
                .did(holderDid)
                .holderName("TCK Holder")
                .build());
    }

    private CreateParticipantContextResponse createParticipantContext(IssuerService issuer, String issuerServiceUrl) {
        var service = issuer.getService(ParticipantContextService.class);
        var vault = issuer.getService(Vault.class);

        var privateKeyAlias = "%s-privatekey-alias".formatted(TEST_PARTICIPANT_CONTEXT_ID);
        vault.storeSecret(TEST_PARTICIPANT_CONTEXT_ID, privateKeyAlias, issuerKey.toJSONString());
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                .did(issuerDid)
                .roles(List.of("admin"))
                .active(true)
                .serviceEndpoint(new Service(UUID.randomUUID().toString(), "IssuerService", issuerServiceUrl))
                .key(KeyDescriptor.Builder.newInstance()
                        .usage(Set.of(CREDENTIAL_SIGNING, TOKEN_SIGNING))
                        .publicKeyJwk(issuerKey.toPublicJWK().toJSONObject())
                        .privateKeyAlias(privateKeyAlias)
                        .keyId(issuerKey.getKeyID())
                        .build())
                .build();
        return service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }
}
