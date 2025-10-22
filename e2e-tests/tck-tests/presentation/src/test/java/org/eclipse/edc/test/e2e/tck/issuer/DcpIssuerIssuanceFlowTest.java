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
import org.assertj.core.api.Assertions;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.e2e.tck.TckTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@TckTest
public class DcpIssuerIssuanceFlowTest {
    @RegisterExtension
    public static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
            .id("issuer-service")
            .name("issuerservice")
            .modules(":dist:bom:issuerservice-bom", ":e2e-tests:tck-tests:test-attestations")
            .build();
    private static final int CALLBACK_PORT = getFreePort();
    private static final String ISSUANCE_CORRELATION_ID = "issuance-correlation-id";
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "issuer";
    public String issuerDid;
    private ECKey issuerKey;

    @BeforeEach
    void setup() {
        issuerDid = ISSUER_EXTENSION.didFor(TEST_PARTICIPANT_CONTEXT_ID);
        issuerKey = generateEcKey(issuerDid + "#key1");
    }

    @DisplayName("Run TCK Issuance tests targeting the Issuer")
    @Test
    void runIssuanceFlowTests(IssuerRuntime runtime) {
        var monitor = new ConsoleMonitor(true, true);

        var issuancePort = ISSUER_EXTENSION.getIssuerApiEndpoint().getUrl().getPort();
        var issuancePath = ISSUER_EXTENSION.getIssuerApiEndpoint().getUrl().getPath();
        var stsPort = ISSUER_EXTENSION.getStsEndpoint().getUrl().getPort();
        var stsPath = ISSUER_EXTENSION.getStsEndpoint().getUrl().getPath();

        var holderDid = "did:web:localhost%%3A%s:holder".formatted(CALLBACK_PORT);

        var baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);
        var baseIssuerServiceUrl = "http://localhost:%s%s/v1alpha/participants/%s".formatted(issuancePort, issuancePath, Base64.encode(TEST_PARTICIPANT_CONTEXT_ID));
        var baseCallbackUri = URI.create(baseCallbackUrl);

        // prepare the issuer service:
        createHolder(runtime, holderDid);
        var response = createParticipantContext(runtime, baseIssuerServiceUrl);
        createDefinitions(runtime);

        var result = TckRuntime.Builder.newInstance()
                .properties(Map.of(
                        "dataspacetck.callback.address", baseCallbackUrl,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.issuer", issuerDid,
                        "dataspacetck.sts.url", "http://localhost:%s%s".formatted(stsPort, stsPath),
                        "dataspacetck.sts.client.id", response.clientId(),
                        "dataspacetck.sts.client.secret", response.clientSecret(),
                        "dataspacetck.credentials.correlation.id", ISSUANCE_CORRELATION_ID
                ))
                .addPackage("org.eclipse.dataspacetck.dcp.verification.issuance.issuer")
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

    private void createDefinitions(IssuerRuntime runtime) {
        var attestationDefinitionService = runtime.getService(AttestationDefinitionService.class);
        attestationDefinitionService.createAttestation(AttestationDefinition.Builder.newInstance()
                        .id("tck-test-attestation")
                        .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                        .attestationType("tck-test").build())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var credentialDefinitionService = runtime.getService(CredentialDefinitionService.class);
        var count = new AtomicInteger(1);
        Stream.of("MembershipCredential", "SensitiveDataCredential").forEach(type ->
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
                        .orElseThrow(f -> new AssertionError(f.getFailureDetail())));
    }

    private void createHolder(IssuerRuntime runtime, String holderDid) {
        var holderStore = runtime.getService(HolderStore.class);
        holderStore.create(Holder.Builder.newInstance()
                .holderId(holderDid)
                .participantContextId(holderDid)
                .did(holderDid)
                .holderName("TCK Holder")
                .build());
    }

    private CreateParticipantContextResponse createParticipantContext(IssuerRuntime runtime, String issuerServiceUrl) {
        var service = runtime.getService(ParticipantContextService.class);
        var vault = runtime.getService(Vault.class);

        var privateKeyAlias = "%s-privatekey-alias".formatted(TEST_PARTICIPANT_CONTEXT_ID);
        vault.storeSecret(privateKeyAlias, issuerKey.toJSONString());
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(TEST_PARTICIPANT_CONTEXT_ID)
                .did(issuerDid)
                .roles(List.of("admin"))
                .active(true)
                .serviceEndpoint(new Service(UUID.randomUUID().toString(), "IssuerService", issuerServiceUrl))
                .key(KeyDescriptor.Builder.newInstance()
                        .usage(Set.of(KeyPairUsage.CREDENTIAL_SIGNING))
                        .publicKeyJwk(issuerKey.toPublicJWK().toJSONObject())
                        .privateKeyAlias(privateKeyAlias)
                        .keyId(issuerKey.getKeyID())
                        .build())
                .build();
        return service.createParticipantContext(manifest)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }
}
