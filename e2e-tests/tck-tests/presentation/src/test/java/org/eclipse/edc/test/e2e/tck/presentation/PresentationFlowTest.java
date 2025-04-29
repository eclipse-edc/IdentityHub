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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import org.assertj.core.api.Assertions;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Asserts the correct functionality of the presentation flow according to the Technology Compatibility Kit (TCK).
 * <p>
 * IdentityHub is started in an in-mem runtime, the TCK is started in another runtime, and executes its test cases against
 * IdentityHubs Presentation API.
 *
 * @see <a href="https://github.com/eclipse-dataspacetck/dcp-tck">Eclipse Dataspace TCK - DCP</a>
 */
public class PresentationFlowTest {
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "holder";
    private static final ECKey HOLDER_KEY = generateEcKey("did:web:localhost%3A19026:holder#key1");
    private static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
    private static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
    private static final int CALLBACK_PORT = getFreePort();
    private static final ScopeToCriterionTransformer TCK_TRANSFORMER = new TckTransformer();
    @RegisterExtension
    static final IdentityHubExtension IDENTITY_HUB_EXTENSION = (IdentityHubExtension) IdentityHubExtension.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .modules(":dist:bom:identityhub-bom")
            .build()
//            .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
            .registerServiceMock(ScopeToCriterionTransformer.class, TCK_TRANSFORMER)
            .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
    public static String holderDid = "did:web:localhost%3A" + CALLBACK_PORT + ":holder";
    private final String baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);

    @BeforeEach
    void setup() throws JOSEException {

        holderDid = IDENTITY_HUB_EXTENSION.didFor(TEST_PARTICIPANT_CONTEXT_ID);
        // set holder configuration
        when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(holderDid))).thenReturn(Result.success(HOLDER_KEY.toPublicKey()));

    }

    @DisplayName("Run TCK tests")
    @Test
    void runDcpTck(IdentityHubRuntime runtime) {
        var monitor = new ConsoleMonitor(true, true);


        var credentialsPort = IDENTITY_HUB_EXTENSION.getCredentialsEndpoint().getUrl().getPort();
        var credentialsPath = IDENTITY_HUB_EXTENSION.getCredentialsEndpoint().getUrl().getPath();

        var stsPort = IDENTITY_HUB_EXTENSION.getStsEndpoint().getUrl().getPort();
        var stsPath = IDENTITY_HUB_EXTENSION.getStsEndpoint().getUrl().getPath();

        var baseCredentialServiceUrl = "http://localhost:%s%s/v1/participants/%s".formatted(credentialsPort, credentialsPath, Base64.encode(TEST_PARTICIPANT_CONTEXT_ID));

        var response = createParticipant(runtime, baseCredentialServiceUrl);
        var result = TckRuntime.Builder.newInstance()
                .properties(Map.of(
                        "dataspacetck.callback.address", baseCallbackUrl,
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.holder", holderDid,
                        "dataspacetck.sts.url", "http://localhost:%s%s".formatted(stsPort, stsPath),
                        "dataspacetck.sts.client.id", response.clientId(),
                        "dataspacetck.sts.client.secret", response.clientSecret()
                ))
                .addPackage("org.eclipse.dataspacetck.dcp.verification")
                .monitor(monitor)
                .build()
                .execute();

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
        vault.storeSecret(privateKeyAlias, HOLDER_KEY.toJSONString());
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .did(holderDid)
                .active(true)
                .serviceEndpoint(new Service(UUID.randomUUID().toString(), "CredentialService", credentialServiceUrl))
                .key(KeyDescriptor.Builder.newInstance()
                        .publicKeyJwk(HOLDER_KEY.toPublicJWK().toJSONObject())
                        .privateKeyAlias(privateKeyAlias)
                        .keyId(HOLDER_KEY.getKeyID())
                        .build())
                .build();
        return service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

}
