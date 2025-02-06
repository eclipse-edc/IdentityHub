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
                new RuntimePerMethodExtension(new EmbeddedRuntime("identityhub-bom",
                        new HashMap<>() {

                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("edc.ih.iam.id", "did:web:test");
                                put("edc.ih.iam.publickey.path", "/some/path/to/key.pem");
                                put("web.http.presentation.port", valueOf(getFreePort()));
                                put("web.http.presentation.path", "/api/resolution");
                                put("web.http.identity.port", valueOf(getFreePort()));
                                put("web.http.identity.path", "/api/identity");
                                put("web.http.version.port", valueOf(getFreePort()));
                                put("web.http.version.path", "/api/version");
                                put("web.http.did.port", valueOf(getFreePort()));
                                put("web.http.did.path", "/api/did");
                                put("edc.sts.account.api.url", "https://sts.com/accounts");
                                put("edc.sts.accounts.api.auth.header.value", "password");
                            }
                        },
                        ":dist:bom:identityhub-bom"
                ));
    }

    @Nested
    @EndToEndTest
    class IdentityHubWithSts extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("identityhub-with-sts-bom",
                        new HashMap<>() {
                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("edc.ih.iam.id", "did:web:test");
                                put("edc.ih.iam.publickey.path", "/some/path/to/key.pem");
                                put("web.http.presentation.port", valueOf(getFreePort()));
                                put("web.http.presentation.path", "/api/resolution");
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
                            }
                        },
                        ":dist:bom:identityhub-with-sts-bom"
                ));
    }

    @Nested
    @EndToEndTest
    class IssuerService extends SmokeTest {
        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("issuer-service-bom",
                        new HashMap<>() {
                            {
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("web.http.version.port", valueOf(getFreePort()));
                                put("web.http.version.path", "/api/version");
                                put("web.http.did.port", valueOf(getFreePort()));
                                put("web.http.did.path", "/api/did");
                                put("web.http.issuer-api.port", valueOf(getFreePort()));
                                put("web.http.issuer-api.path", "/api/issuer");
                                put("edc.sts.account.api.url", "https://sts.com/accounts");
                                put("edc.sts.accounts.api.auth.header.value", "password");
                                put("edc.issuer.statuslist.signing.key.alias", "signing-key");
                            }
                        },
                        ":dist:bom:issuerservice-bom"
                ));
    }
}
