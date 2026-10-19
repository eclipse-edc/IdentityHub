/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.http;

import com.sun.net.httpserver.HttpServer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class IssuerEndpointsTest {

    private final List<String> received = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            received.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            if (exchange.getRequestURI().getPath().equals("/redirect")) {
                exchange.getResponseHeaders().add("Location", "http://localhost:%d/internal".formatted(port));
                exchange.sendResponseHeaders(302, -1);
            } else {
                exchange.sendResponseHeaders(200, -1);
            }
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://issuer.com/api/issuance",
            "https://93.184.215.14/api/issuance",
            "https://[2606:4700::1]/api/issuance"
    })
    void check_whenPublicHttps_shouldSucceed(String url) {
        assertThat(IssuerEndpoints.check(url, Set.of())).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://issuer.com/api/issuance",
            "https://127.0.0.1/api/issuance",
            "https://0.0.0.0/api/issuance",
            "https://10.1.2.3/api/issuance",
            "https://172.16.0.1/api/issuance",
            "https://192.168.1.1/api/issuance",
            "https://169.254.169.254/latest/meta-data",
            "https://100.64.0.1/api/issuance",
            "https://[::1]/api/issuance",
            "https://[fe80::1]/api/issuance",
            "https://[fd12:3456::1]/api/issuance",
            "https://[::ffff:127.0.0.1]/api/issuance",
            "file:///etc/passwd",
            "not a url"
    })
    void check_whenNotPublicHttps_shouldFail(String url) {
        assertThat(IssuerEndpoints.check(url, Set.of())).isFailed();
    }

    @Test
    void check_whenHostAllowListed_shouldSucceed() {
        var allowedHosts = IssuerEndpoints.parseAllowedHosts(" issuer-service , 10.1.2.3,[::1]");

        assertThat(IssuerEndpoints.check("http://Issuer-Service:8080/api/issuance", allowedHosts)).isSucceeded();
        assertThat(IssuerEndpoints.check("http://10.1.2.3/api/issuance", allowedHosts)).isSucceeded();
        assertThat(IssuerEndpoints.check("http://[0:0:0:0:0:0:0:1]/api/issuance", allowedHosts)).isSucceeded();
        assertThat(IssuerEndpoints.check("http://10.1.2.4/api/issuance", allowedHosts)).isFailed();
    }

    @Test
    void restrict_whenHostResolvesToLoopback_shouldNotConnect() {
        var client = IssuerEndpoints.restrict(new OkHttpClient(), Set.of());

        // the URL itself passes the check, but the host resolves to a loopback address
        assertThatThrownBy(() -> client.newCall(new Request.Builder().url("https://localhost:%d/".formatted(port)).build()).execute())
                .isInstanceOf(UnknownHostException.class)
                .hasMessageContaining("does not resolve to any public address");
        assertThat(received).isEmpty();
    }

    @Test
    void restrict_whenNotHttps_shouldNotConnect() {
        var client = IssuerEndpoints.restrict(new OkHttpClient(), Set.of());

        assertThatThrownBy(() -> client.newCall(new Request.Builder().url("http://127.0.0.1:%d/".formatted(port)).build()).execute())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("does not use HTTPS");
        assertThat(received).isEmpty();
    }

    @Test
    void restrict_whenHostAllowListed_shouldConnect() throws IOException {
        var client = IssuerEndpoints.restrict(new OkHttpClient(), Set.of("localhost"));

        try (var response = client.newCall(new Request.Builder().url("http://localhost:%d/api".formatted(port)).build()).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
        assertThat(received).containsExactly("GET /api");
    }

    @Test
    void restrict_shouldNotFollowRedirects() throws IOException {
        var client = IssuerEndpoints.restrict(new OkHttpClient(), Set.of("localhost"));
        var request = new Request.Builder()
                .url("http://localhost:%d/redirect".formatted(port))
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .build();

        try (var response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }
        assertThat(received).containsExactly("POST /redirect");
    }

    @Test
    void readBody_shouldReadBody() throws IOException {
        var body = ResponseBody.create("issuer-pid", MediaType.parse("text/plain"));

        assertThat(IssuerEndpoints.readBody(body)).isEqualTo("issuer-pid");
    }

    @Test
    void readBody_whenTooLarge_shouldFail() {
        var content = new Buffer().write(new byte[(int) IssuerEndpoints.MAX_RESPONSE_SIZE + 1]);
        // without a content length, so that the size is only known once the body is read
        var body = ResponseBody.create(content, MediaType.parse("text/plain"), -1);

        assertThatThrownBy(() -> IssuerEndpoints.readBody(body))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exceeds");
    }

}
