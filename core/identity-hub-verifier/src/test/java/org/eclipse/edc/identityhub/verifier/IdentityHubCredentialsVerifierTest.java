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

package org.eclipse.edc.identityhub.verifier;

import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifier;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifierRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityHubCredentialsVerifierTest {

    private static final String HUB_BASE_URL = "https://" + "http://some.test.url";
    private static final DidDocument DID_DOCUMENT = DidDocument.Builder.newInstance()
            .service(List.of(new Service("IdentityHub", "IdentityHub", HUB_BASE_URL))).build();

    private final Monitor monitorMock = mock(Monitor.class);
    private final IdentityHubClient identityHubClientMock = mock(IdentityHubClient.class);
    private final CredentialEnvelopeVerifierRegistry verifierRegistry = mock(CredentialEnvelopeVerifierRegistry.class);
    private final CredentialsVerifier credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClientMock, monitorMock, verifierRegistry);


    @Test
    void getVerifiableCredentials_shouldReturnError_withEmptyRegistry() {

        var envelope = mock(CredentialEnvelope.class);
        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.success(List.of(envelope)));
        var result = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void getVerifiableCredentials_shouldReturnError_FailedRequest() {

        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failure"));
        var result = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void getVerifiableCredentials_shouldReturnEmptyCredentials() {

        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.success(Collections.emptyList()));
        var result = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        assertThat(result.succeeded()).isTrue();

        assertThat(result.getContent().size()).isZero();
    }

    @Test
    void getVerifiableCredentials_shouldReturnValidCredentials() {

        var envelope = mock(CredentialEnvelope.class);
        var verifier = mock(CredentialEnvelopeVerifier.class);

        when(identityHubClientMock.getVerifiableCredentials(HUB_BASE_URL)).thenReturn(StatusResult.success(List.of(envelope)));
        when(verifierRegistry.resolve(any())).thenReturn(verifier);
        var credential = VerifiableCredentialTestUtil.generateCredential();
        when(verifier.verify(envelope, DID_DOCUMENT)).thenReturn(Result.success(credential));

        var result = credentialsVerifier.getVerifiedCredentials(DID_DOCUMENT);

        assertThat(result.succeeded()).isTrue();

        var content = result.getContent();

        assertThat(content).hasSize(1)
                .extractingByKey(credential.getId()).satisfies(obj -> {
                    assertThat(obj).isInstanceOf(Credential.class);
                    var cred = (Credential) obj;
                    assertThat(cred).usingRecursiveComparison().isEqualTo(credential);
                });
    }


}
