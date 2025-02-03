/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * The IssuerServiceRuntimeConfiguration class represents an IssuerService Runtime configuration and provides various information, such as API endpoints
 */
public class IssuerServiceRuntimeConfiguration {

    private Endpoint adminEndpoint;
    private Endpoint issuerApiEndpoint;
    private String id;
    private String name;

    public Endpoint getAdminEndpoint() {
        return adminEndpoint;
    }

    public Endpoint getIssuerApiEndpoint() {
        return issuerApiEndpoint;
    }

    public Map<String, String> config() {
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.issueradmin.port", String.valueOf(adminEndpoint.getUrl().getPort()));
                put("web.http.issueradmin.path", adminEndpoint.getUrl().getPath());

                put("web.http.issuer-api.port", String.valueOf(issuerApiEndpoint.getUrl().getPort()));
                put("web.http.issuer-api.path", issuerApiEndpoint.getUrl().getPath());
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/.well-known/api");
                put("web.http.did.port", String.valueOf(getFreePort()));
                put("web.http.did.path", "/");
                put("edc.ih.iam.id", "did:web:consumer");
                put("edc.sql.schema.autocreate", "true");
                put("edc.sts.account.api.url", "http://sts.com/accounts");
                put("edc.sts.accounts.api.auth.header.value", "password");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
            }
        };
    }


    public static final class Builder {
        private final IssuerServiceRuntimeConfiguration participant;

        private Builder() {
            participant = new IssuerServiceRuntimeConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.participant.id = id;
            return this;
        }

        public Builder name(String name) {
            this.participant.name = name;
            return this;
        }

        public IssuerServiceRuntimeConfiguration build() {
            participant.adminEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/admin"), Map.of());
            participant.issuerApiEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/issuance"), Map.of());
            return participant;
        }
    }

    public static class Endpoint {
        private final URI url;
        private final Map<String, String> headers;

        public Endpoint(URI url, Map<String, String> headers) {
            this.url = url;
            this.headers = headers;
        }

        public RequestSpecification baseRequest() {
            return given().baseUri(this.url.toString()).headers(this.headers);
        }

        public URI getUrl() {
            return this.url;
        }
    }
}
