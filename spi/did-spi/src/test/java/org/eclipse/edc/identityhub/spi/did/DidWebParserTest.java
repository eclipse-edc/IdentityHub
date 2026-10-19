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

package org.eclipse.edc.identityhub.spi.did;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DidWebParserTest {
    private final DidWebParser parser = new DidWebParser();

    @ParameterizedTest
    @CsvSource({
            "http://localhost:123, did:web:localhost%3A123",
            "http://localhost:123/, did:web:localhost%3A123",
            "http://localhost:123/did.json, did:web:localhost%3A123",
            "http://localhost:123/.well-known/did.json, did:web:localhost%3A123",
            "http://localhost:123/asdf/gh, did:web:localhost%3A123:asdf:gh",
            "http://localhost:123/asdf/gh/did.json, did:web:localhost%3A123:asdf:gh",
            "http://localhost:123/asdf/gh/.well-known/did.json, did:web:localhost%3A123:asdf:gh",
            "http://localhost:123/asdf/ghdid.json, did:web:localhost%3A123:asdf:ghdid.json"
    })
    void parse(String url, String expectedDid) {
        assertThat(parser.parse(URI.create(url), StandardCharsets.UTF_8)).isEqualTo(expectedDid);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:123/asdf/gh/",
            "http://localhost:123/asdf//gh",
            "http://localhost:123/asdf/gh/did.json/",
            "http://localhost:123/asdf/gh/did.json/did.json",
            "http://localhost:123/asdf/did.json/gh",
            "http://localhost:123/.well-known",
            "http://localhost:123/asdf/gh/.well-known",
            "http://localhost:123/asdf/gh/.well-known/",
            "http://localhost:123/asdf/gh/.well-known/did.json/",
            "http://localhost:123/asdf/.well-known/gh/did.json",
            "http://localhost:123/.well-known/.well-known/did.json",
            "http://localhost:123/asdf/gh/.well-known/did.json/asdf"
    })
    void parse_unservedUrl_returnsNull(String url) {
        assertThat(parser.parse(URI.create(url), StandardCharsets.UTF_8)).isNull();
    }
}
