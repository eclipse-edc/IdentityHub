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

package org.eclipse.edc.identityhub.tests.fixtures.common;

import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class Endpoint {
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
