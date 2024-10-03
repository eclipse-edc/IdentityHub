/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.sts.accountservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;

import java.util.Map;

import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.stop.Stop.stopQuietly;

@ComponentTest
class RemoteStsAccountServiceTest {

    public static final RecursiveComparisonConfiguration TEST_CONFIGURATION = RecursiveComparisonConfiguration.builder().withIgnoredFields("clock").build();
    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String PARTICIPANT_DID = "did:web:" + PARTICIPANT_CONTEXT_ID;
    private static final String KEY_ID = "test-key-id";
    private final int port = getFreePort();
    private final EdcHttpClient http = testHttpClient();
    private RemoteStsAccountService remoteStsAccountService;
    private ClientAndServer server;

    @BeforeEach
    public void startServer() {
        server = ClientAndServer.startClientAndServer(port);
        var url = "http://localhost:" + port;
        remoteStsAccountService = new RemoteStsAccountService(url, http, Map::of, mock(), new ObjectMapper());
    }

    @AfterEach
    public void stopServer() {
        stopQuietly(server);
    }

    @Test
    void create() {
        server.when(request().withMethod("POST").withPath("/v1alpha/accounts"))
                .respond(HttpResponse.response().withStatusCode(204));

        assertThat(remoteStsAccountService.createAccount(createManifest().build(), "test-alias")).isSucceeded();

        server.verify(request().withMethod("POST").withPath("/v1alpha/accounts"));
    }

    @ParameterizedTest(name = "HTTP Error {0}")
    @ValueSource(ints = { 400, 401, 404, 409, 500 })
    void create_withErrorCode(int errorCode) {
        server.when(request().withMethod("POST").withPath("/v1alpha/accounts"))
                .respond(HttpResponse.response().withStatusCode(errorCode));

        assertThat(remoteStsAccountService.createAccount(createManifest().build(), "test-alias")).isFailed();
        server.verify(request().withMethod("POST").withPath("/v1alpha/accounts"));
    }

    @Test
    void findById() {
        var account = createAccount().build();
        server.when(request().withMethod("GET").withPath("/v1alpha/accounts/test-id"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(JsonBody.json(account)));

        assertThat(remoteStsAccountService.findById("test-id")).isSucceeded()
                .usingRecursiveComparison(TEST_CONFIGURATION)
                .isEqualTo(account);

        server.verify(request().withMethod("GET").withPath("/v1alpha/accounts/test-id"));
    }

    @ParameterizedTest(name = "HTTP Error {0}")
    @ValueSource(ints = { 400, 401, 404, 409, 500 })
    void findById_withErrorCode(int errorCode) {
        server.when(request().withMethod("GET").withPath("/v1alpha/accounts/test-id"))
                .respond(HttpResponse.response().withStatusCode(errorCode));

        assertThat(remoteStsAccountService.findById("test-id")).isFailed();

        server.verify(request().withMethod("GET").withPath("/v1alpha/accounts/test-id"));
    }

    @Test
    void update_succeeds() {
        var account = createAccount().build();
        server.when(request().withMethod("PUT").withPath("/v1alpha/accounts"))
                .respond(HttpResponse.response().withStatusCode(200));

        assertThat(remoteStsAccountService.updateAccount(account)).isSucceeded();

        server.verify(request().withMethod("PUT").withPath("/v1alpha/accounts"));
    }

    @ParameterizedTest(name = "HTTP Error {0}")
    @ValueSource(ints = { 400, 401, 404, 409, 500 })
    void update_succeeds(int errorCode) {
        var account = createAccount().build();
        server.when(request().withMethod("PUT").withPath("/v1alpha/accounts"))
                .respond(HttpResponse.response().withStatusCode(errorCode));

        assertThat(remoteStsAccountService.updateAccount(account)).isFailed();

        server.verify(request().withMethod("PUT").withPath("/v1alpha/accounts"));
    }

    @Test
    void delete_succeeds() {
        server.when(request().withMethod("DELETE").withPath("/v1alpha/accounts/test-id"))
                .respond(HttpResponse.response().withStatusCode(200));

        assertThat(remoteStsAccountService.deleteAccount("test-id")).isSucceeded();

        server.verify(request().withMethod("DELETE").withPath("/v1alpha/accounts/test-id"));
    }

    @ParameterizedTest(name = "HTTP Error {0}")
    @ValueSource(ints = { 400, 401, 404, 409, 500 })
    void delete_succeeds(int errorCode) {
        server.when(request().withMethod("DELETE").withPath("/v1alpha/accounts/test-id"))
                .respond(HttpResponse.response().withStatusCode(errorCode));

        assertThat(remoteStsAccountService.deleteAccount("test-id")).isFailed();

        server.verify(request().withMethod("DELETE").withPath("/v1alpha/accounts/test-id"));
    }

    private StsAccount.Builder createAccount() {
        return StsAccount.Builder.newInstance()
                .id("test-id")
                .name("test-name")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .secretAlias("test-secret")
                .publicKeyReference("public-key-ref")
                .privateKeyAlias("private-key-alias")
                .clientId("client-id");
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .active(true)
                .did(PARTICIPANT_DID)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(KEY_ID + "-alias")
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId(KEY_ID)
                        .build()
                );
    }
}