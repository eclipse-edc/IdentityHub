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

package org.eclipse.edc.test.e2e.tck.presentation;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import org.assertj.core.api.Assertions;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
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
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.e2e.tck.TckTest;
import org.eclipse.edc.test.e2e.tck.TckTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;

/**
 * Asserts the correct functionality of the presentation flow according to the Technology Compatibility Kit (TCK).
 * <p>
 * IdentityHub is started in an in-mem runtime, the TCK is started in another runtime, and executes its test cases against
 * IdentityHubs Presentation API.
 *
 * @see <a href="https://github.com/eclipse-dataspacetck/dcp-tck">Eclipse Dataspace TCK - DCP</a>
 */
@TckTest
public class DcpPresentationFlowTest {
    public static final String ISSUANCE_CORRELATION_ID = "issuance-correlation-id";
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "holder";
    private static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
    private static final int CALLBACK_PORT = getFreePort();
    private static final ScopeToCriterionTransformer TCK_TRANSFORMER = new TckTransformer();
    @RegisterExtension
    static final IdentityHubExtension IDENTITY_HUB_EXTENSION = (IdentityHubExtension) IdentityHubExtension.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .modules(":dist:bom:identityhub-bom")
            .build()
            .registerServiceMock(ScopeToCriterionTransformer.class, TCK_TRANSFORMER)
            .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
    private static final String ISSUER_DID = "did:web:issuer";
    public String holderDid;
    private ECKey holderKey;

    @BeforeEach
    void setup(HolderCredentialRequestStore requestStore) {

        holderDid = IDENTITY_HUB_EXTENSION.didFor(TEST_PARTICIPANT_CONTEXT_ID);
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

    @DisplayName("Run TCK Presentation Flow tests")
    @Test
    void runPresentationFlowTests(IdentityHubRuntime runtime) {
        var monitor = new ConsoleMonitor(true, true);

        var credentialsPort = IDENTITY_HUB_EXTENSION.getCredentialsEndpoint().getUrl().getPort();
        var credentialsPath = IDENTITY_HUB_EXTENSION.getCredentialsEndpoint().getUrl().getPath();

        var stsPort = IDENTITY_HUB_EXTENSION.getStsEndpoint().getUrl().getPort();
        var stsPath = IDENTITY_HUB_EXTENSION.getStsEndpoint().getUrl().getPath();

        var baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);
        var baseCredentialServiceUrl = "http://localhost:%s%s/v1/participants/%s".formatted(credentialsPort, credentialsPath, Base64.encode(TEST_PARTICIPANT_CONTEXT_ID));
        var baseCallbackUri = URI.create(baseCallbackUrl);

        var response = createParticipant(runtime, baseCredentialServiceUrl);
        var result = TckRuntime.Builder.newInstance()
                .properties(Map.of(
                        "dataspacetck.callback.address", baseCallbackUrl,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.holder", holderDid,
                        "dataspacetck.sts.url", "http://localhost:%s%s".formatted(stsPort, stsPath),
                        "dataspacetck.sts.client.id", response.clientId(),
                        "dataspacetck.sts.client.secret", response.clientSecret(),
                        "dataspacetck.credentials.correlation.id", ISSUANCE_CORRELATION_ID
                ))
                .addPackage("org.eclipse.dataspacetck.dcp.verification.presentation.cs")
                .monitor(monitor)
                .build()
                .execute();

        monitor.enableBold().message("DCP Tests done: %s succeeded, %s failed".formatted(
                result.getTestsSucceededCount(), result.getTotalFailureCount()
        )).resetMode();
        if (!result.getFailures().isEmpty()) {
            var failures = result.getFailures().stream()
                    .map(f -> "- " + f.getTestIdentifier().getDisplayName() + " (" + f.getException() + ")")
                    .collect(Collectors.joining("\n"));
            Assertions.fail(result.getTotalFailureCount() + " TCK test cases failed:\n" + failures);
        }
    }

    private CreateParticipantContextResponse createParticipant(IdentityHubRuntime runtime, String credentialServiceUrl) {
        var service = runtime.getService(ParticipantContextService.class);
        var vault = runtime.getService(Vault.class);

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
