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
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil;

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

    private Endpoint resolutionEndpoint;
    private Endpoint managementEndpoint;
    private String id;
    private String name;

    public Endpoint getResolutionEndpoint() {
        return resolutionEndpoint;
    }

    public Map<String, String> controlPlaneConfiguration() {
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.resolution.port", String.valueOf(resolutionEndpoint.getUrl().getPort()));
                put("web.http.resolution.path", resolutionEndpoint.getUrl().getPath());
                put("web.http.identity.port", String.valueOf(managementEndpoint.getUrl().getPort()));
                put("web.http.identity.path", managementEndpoint.getUrl().getPath());
                put("edc.connector.name", name);
                put("edc.ih.iam.publickey.alias", JwtCreationUtil.CONSUMER_KEY.getKeyID());
                put("edc.ih.iam.id", "did:web:consumer");
            }
        };
    }

    public Endpoint getManagementEndpoint() {
        return managementEndpoint;
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
            participant.resolutionEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/v1/resolution"), Map.of());
            participant.managementEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), Map.of());
            return participant;
        }
    }

    public static class Endpoint {
        private final URI url;
        private final Map<String, String> headers;

        public Endpoint(URI url) {
            this.url = url;
            this.headers = new HashMap();
        }

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
