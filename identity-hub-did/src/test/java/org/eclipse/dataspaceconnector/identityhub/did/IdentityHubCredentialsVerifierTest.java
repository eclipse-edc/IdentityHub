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

package org.eclipse.dataspaceconnector.identityhub.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toPublicKeyWrapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityHubCredentialsVerifierTest {

    private static final Faker FAKER = new Faker();

    @Test
    public void getVerifiedClaims_getValidClaims() throws Exception {

        var hubBaseUrl = "https://" + FAKER.internet().url();
        var issuer = FAKER.internet().url();
        var jwk = generateEcKey();

        var identityHubClient = mock(IdentityHubClient.class);
        var monitor = new ConsoleMonitor();
        var didPublicKeyResolver = mock(DidPublicKeyResolver.class);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, monitor, didPublicKeyResolver);
        var didDocument = DidDocument.Builder.newInstance().service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        var credential = generateVerifiableCredential();

        var jws = buildSignedJwt(credential, issuer, jwk);
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl))
                .thenReturn(StatusResult.success(List.of(jws)));

        when(didPublicKeyResolver.resolvePublicKey(issuer))
                .thenReturn(Result.success(toPublicKeyWrapper(jwk)));
        var credentials = credentialsVerifier.verifyCredentials(didDocument);

        assertThat(credentials.succeeded());
        assertThat(credentials.getContent())
                .usingRecursiveComparison()
                .ignoringFields(String.format("%s.exp", credential.getId()))
                .isEqualTo(toMap(credential, issuer));
    }

    @Test
    public void getVerifiedClaims_filtersSignedByWrongIssuer() {

    }

    @Test
    public void getVerifiedClaims_filtersClaimsWithWrongFormat() {

    }
}
