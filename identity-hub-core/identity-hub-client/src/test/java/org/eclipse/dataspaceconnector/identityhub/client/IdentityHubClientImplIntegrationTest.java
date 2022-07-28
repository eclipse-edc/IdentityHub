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

package org.eclipse.dataspaceconnector.identityhub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

@ExtendWith(EdcExtension.class)
public class IdentityHubClientImplIntegrationTest {

    private static final String API_URL = "http://localhost:8181/api/identity-hub";
    private static final Faker FAKER = new Faker();
    private static final VerifiableCredential VERIFIABLE_CREDENTIAL = VerifiableCredential.Builder.newInstance().id(FAKER.internet().uuid()).build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private IdentityHubClient client;

    @BeforeEach
    void setUp() {
        var okHttpClient = TestUtils.testOkHttpClient();
        client = new IdentityHubClientImpl(okHttpClient, OBJECT_MAPPER, new ConsoleMonitor());
    }

    @Test
    void addAndQueryVerifiableCredentials() throws Exception {
        var jws = buildSignedJwt(VERIFIABLE_CREDENTIAL, FAKER.internet().url(), FAKER.internet().url(), generateEcKey());

        addVerifiableCredential(jws);
        getVerifiableCredential(jws);
    }

    private void addVerifiableCredential(SignedJWT jws) throws IOException {
        var statusResult = client.addVerifiableCredential(API_URL, jws);
        assertThat(statusResult.succeeded()).isTrue();
    }

    private void getVerifiableCredential(SignedJWT jws) throws IOException {
        var statusResult = client.getVerifiableCredentials(API_URL);
        assertThat(statusResult.succeeded()).isTrue();
        assertThat(statusResult.getContent()).usingRecursiveFieldByFieldElementComparator().contains(jws);
    }
}
