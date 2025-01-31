/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.identityhub.tests.fixtures;

import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * The IdentityHubRuntimeConfiguration class represents an IdentityHub Runtime configuration and provides various information, such as API endpoints
 */
public class IdentityHubRuntimeConfiguration {

    private Endpoint presentationEndpoint;
    private Endpoint identityEndpoint;
    private String id;
    private String name;

    public Endpoint getPresentationEndpoint() {
        return presentationEndpoint;
    }

    public Map<String, String> config() {
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.presentation.port", String.valueOf(presentationEndpoint.getUrl().getPort()));
                put("web.http.presentation.path", presentationEndpoint.getUrl().getPath());
                put("web.http.identity.port", String.valueOf(identityEndpoint.getUrl().getPort()));
                put("web.http.identity.path", identityEndpoint.getUrl().getPath());
                put("web.http.sts.port", String.valueOf(getFreePort()));
                put("web.http.sts.path", "/api/sts");
                put("web.http.accounts.port", String.valueOf(getFreePort()));
                put("web.http.accounts.path", "/api/accounts");
                put("edc.runtime.id", name);
                put("edc.ih.iam.id", "did:web:consumer");
                put("edc.sql.schema.autocreate", "true");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
            }
        };
    }

    public Endpoint getIdentityApiEndpoint() {
        return identityEndpoint;
    }

    public static final class Builder {
        private final IdentityHubRuntimeConfiguration participant;

        private Builder() {
            participant = new IdentityHubRuntimeConfiguration();
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

        public IdentityHubRuntimeConfiguration build() {
            participant.presentationEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/presentation"), Map.of());
            participant.identityEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/identity"), Map.of());
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
