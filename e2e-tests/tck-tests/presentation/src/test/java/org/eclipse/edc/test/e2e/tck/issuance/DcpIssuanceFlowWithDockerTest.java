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

package org.eclipse.edc.test.e2e.tck.issuance;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.e2e.tck.TckTest;
import org.eclipse.edc.test.e2e.tck.TckTransformer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;

@TckTest
@Testcontainers
public class DcpIssuanceFlowWithDockerTest {
    private static final String ISSUANCE_CORRELATION_ID = "issuance-correlation-id";
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "holder";
    private static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
    private static final int CALLBACK_PORT = getFreePort();
    private static final ScopeToCriterionTransformer TCK_TRANSFORMER = new TckTransformer();
    private static final Endpoints ENDPOINTS = Endpoints.Builder.newInstance()
            .endpoint("credentials", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/credentials"))
            .endpoint("sts", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/sts"))
            .endpoint("issuance", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/issuance"))
            .endpoint("statuslist", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/identity"))
            .endpoint("identity", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/api/statuslist"))
            .endpoint("did", () -> URI.create("http://host.docker.internal:" + getFreePort() + "/"))
            .build();

    @RegisterExtension
    static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
            .name("identity-hub")
            .modules(DefaultRuntimes.IdentityHub.MODULES)
            .endpoints(ENDPOINTS)
            .configurationProvider(DefaultRuntimes.IdentityHub::config)
            .paramProvider(IdentityHub.class, IdentityHub::forContext)
            .build()
            .registerServiceMock(ScopeToCriterionTransformer.class, TCK_TRANSFORMER)
            .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
    private static final String ISSUER_DID = "did:web:issuer";
    public String holderDid;
    private ECKey holderKey;

    @BeforeEach
    void setup(IdentityHub identityHub, HolderCredentialRequestStore requestStore) {

        holderDid = identityHub.didFor(TEST_PARTICIPANT_CONTEXT_ID);
        holderKey = generateEcKey(holderDid + "#key1");

        // fake credentials
        requestStore.save(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                .requestId(ISSUANCE_CORRELATION_ID)
                .state(HolderRequestState.REQUESTED.code())
                .issuerPid(UUID.randomUUID().toString())
                .requestedCredential("membershipCredential-id", "MembershipCredential", "VC1_0_JWT")
                .requestedCredential("sensitiveDataCredential-id", "SensitiveDataCredential", "VC1_0_JWT")
                .build());
    }

    @DisplayName("Run TCK Issuance Flow tests (using docker)")
    @Test
    void runIssuanceFlowTests(IdentityHub identityHub) throws InterruptedException {

        var monitor = new ConsoleMonitor("TCK", ConsoleMonitor.Level.DEBUG, true);
        var credentialsPort = identityHub.getCredentialsEndpoint().getUrl().getPort();
        var credentialsPath = identityHub.getCredentialsEndpoint().getUrl().getPath();

        var stsPort = identityHub.getStsEndpoint().getUrl().getPort();
        var stsPath = identityHub.getStsEndpoint().getUrl().getPath();

        var baseCallbackAddress = "http://0.0.0.0:%s".formatted(CALLBACK_PORT);
        var baseCredentialServiceUrl = "http://host.docker.internal:%s%s/v1/participants/%s".formatted(credentialsPort, credentialsPath, Base64.encode(TEST_PARTICIPANT_CONTEXT_ID));
        var baseCallbackUri = URI.create(baseCallbackAddress);

        var response = createParticipant(identityHub, baseCredentialServiceUrl);

        try (var tckContainer = new GenericContainer<>("eclipsedataspacetck/dcp-tck-runtime:1.0.0-RC3")
                .withExtraHost("host.docker.internal", "host-gateway")
                .withExposedPorts(CALLBACK_PORT)
                .withEnv(Map.of(
                        "dataspacetck.callback.address", baseCallbackAddress,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.holder", holderDid,
                        "dataspacetck.sts.url", "http://host.docker.internal:%s%s".formatted(stsPort, stsPath),
                        "dataspacetck.sts.client.id", response.clientId(),
                        "dataspacetck.sts.client.secret", response.clientSecret(),
                        "dataspacetck.credentials.correlation.id", ISSUANCE_CORRELATION_ID,
                        "dataspacetck.test.package", "org.eclipse.dataspacetck.dcp.verification.issuance.cs"
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

    private CreateParticipantContextResponse createParticipant(IdentityHub identityHub, String credentialServiceUrl) {
        var service = identityHub.getService(ParticipantContextService.class);
        var vault = identityHub.getService(Vault.class);

        var privateKeyAlias = "%s-privatekey-alias".formatted(TEST_PARTICIPANT_CONTEXT_ID);
        vault.storeSecret(privateKeyAlias, holderKey.toJSONString());
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .did(holderDid)
                .active(true)
                .serviceEndpoint(new Service(UUID.randomUUID().toString(), "CredentialService", credentialServiceUrl))
                .key(KeyDescriptor.Builder.newInstance()
                        .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                        .publicKeyJwk(holderKey.toPublicJWK().toJSONObject())
                        .privateKeyAlias(privateKeyAlias)
                        .keyId(holderKey.getKeyID())
                        .build())
                .build();
        return service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

}
