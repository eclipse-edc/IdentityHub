/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistry;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
class IdentityHubClientImplIntegrationTest {

    private static final String API_URL = "http://localhost:8181/api/identity-hub";
    private static final VerifiableCredential VERIFIABLE_CREDENTIAL = VerifiableCredential.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .credentialSubject(Map.of("foo", "bar"))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final CredentialEnvelopeTransformerRegistry registry = mock(CredentialEnvelopeTransformerRegistry.class);
    private IdentityHubClient client;

    @BeforeEach
    void setUp() {
        var okHttpClient = TestUtils.testOkHttpClient();
        when(registry.resolve(any())).thenReturn(new JwtCredentialEnvelopeTransformer(OBJECT_MAPPER));
        client = new IdentityHubClientImpl(okHttpClient, OBJECT_MAPPER, mock(Monitor.class), registry);
    }

    @Test
    void getSelfDescription() {
        var statusResult = client.getSelfDescription(API_URL);

        assertThat(statusResult.succeeded()).isTrue();
        assertThat(statusResult.getContent().get("selfDescriptionCredential")).isNotNull();
    }

    @Test
    void addAndQueryVerifiableCredentials() {
        var jws = buildSignedJwt(VERIFIABLE_CREDENTIAL, "http://test.url", "http://some.test.url", generateEcKey());

        var jwsEnvelope = new JwtCredentialEnvelope(jws);
        addVerifiableCredential(jwsEnvelope);
        getVerifiableCredential(jwsEnvelope);
    }

    private void addVerifiableCredential(CredentialEnvelope jws) {
        var statusResult = client.addVerifiableCredential(API_URL, jws);
        assertThat(statusResult.succeeded()).isTrue();
    }

    private void getVerifiableCredential(CredentialEnvelope jws) {
        var statusResult = client.getVerifiableCredentials(API_URL);
        assertThat(statusResult.succeeded()).isTrue();
        assertThat(statusResult.getContent()).usingRecursiveFieldByFieldElementComparator().contains(jws);
    }
}
