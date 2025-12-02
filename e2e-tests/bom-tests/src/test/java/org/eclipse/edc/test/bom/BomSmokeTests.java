/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.bom;


import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static java.lang.String.valueOf;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.equalTo;


public class BomSmokeTests {
    abstract static class SmokeTest {
        public static final String DEFAULT_PORT = "8080";
        public static final String DEFAULT_PATH = "/api";

        @Test
        void assertRuntimeReady() {
            await().untilAsserted(() -> given()
                    .baseUri("http://localhost:" + DEFAULT_PORT + DEFAULT_PATH + "/check/startup")
                    .get()
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .body("isSystemHealthy", equalTo(true)));

        }
    }

    @Nested
    @EndToEndTest
    class IdentityHub extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("identityhub-bom", ":dist:bom:identityhub-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("edc.ih.iam.publickey.path", "/some/path/to/key.pem");
                                put("web.http.credentials.port", valueOf(getFreePort()));
                                put("web.http.credentials.path", "/api/credentials");
                                put("web.http.identity.port", valueOf(getFreePort()));
                                put("web.http.identity.path", "/api/identity");
                                put("web.http.accounts.port", valueOf(getFreePort()));
                                put("web.http.accounts.path", "/api/accounts");
                                put("web.http.version.port", valueOf(getFreePort()));
                                put("web.http.version.path", "/api/version");
                                put("web.http.sts.port", valueOf(getFreePort()));
                                put("web.http.sts.path", "/api/sts");
                                put("web.http.did.port", valueOf(getFreePort()));
                                put("web.http.did.path", "/api/did");
                                // interaction with embedded STS
                                put("edc.iam.sts.publickey.id", "test-public-key");
                                put("edc.iam.sts.privatekey.alias", "test-private-key");

                            }
                        })));
    }

    @Nested
    @EndToEndTest
    class IdentityHubOauth2 extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("identityhub-oauth2-bom", ":dist:bom:identityhub-oauth2-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("edc.ih.iam.publickey.path", "/some/path/to/key.pem");
                                put("web.http.credentials.port", valueOf(getFreePort()));
                                put("web.http.credentials.path", "/api/credentials");
                                put("web.http.identity.port", valueOf(getFreePort()));
                                put("web.http.identity.path", "/api/identity");
                                put("web.http.accounts.port", valueOf(getFreePort()));
                                put("web.http.accounts.path", "/api/accounts");
                                put("web.http.version.port", valueOf(getFreePort()));
                                put("web.http.version.path", "/api/version");
                                put("web.http.sts.port", valueOf(getFreePort()));
                                put("web.http.sts.path", "/api/sts");
                                put("web.http.did.port", valueOf(getFreePort()));
                                put("web.http.did.path", "/api/did");
                                // interaction with embedded STS
                                put("edc.iam.sts.publickey.id", "test-public-key");
                                put("edc.iam.sts.privatekey.alias", "test-private-key");

                                //this is specific to the oauth2-bom
                                put("edc.iam.oauth2.issuer", "your-expected-issuer");
                                put("edc.iam.oauth2.jwks.url", "https://example.com/jwks.json");
                            }
                        })));
    }

    @Nested
    @EndToEndTest
    class IssuerService extends SmokeTest {
        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("issuer-service-bom", ":dist:bom:issuerservice-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("web.http.version.port", valueOf(getFreePort()));
                                put("web.http.version.path", "/api/version");
                                put("web.http.did.port", valueOf(getFreePort()));
                                put("web.http.did.path", "/api/did");
                                put("web.http.issuance.port", valueOf(getFreePort()));
                                put("edc.sts.account.api.url", "https://sts.com/accounts");
                                put("edc.sts.accounts.api.auth.header.value", "password");
                                put("edc.issuer.statuslist.signing.key.alias", "signing-key");
                                // interaction with embedded STS
                                put("edc.iam.sts.publickey.id", "test-public-key");
                                put("edc.iam.sts.privatekey.alias", "test-private-key");
                            }
                        })));
    }
}
