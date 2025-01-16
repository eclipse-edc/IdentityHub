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

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

class DidWebParserTest {
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    private final DidWebParser parser = new DidWebParser();

    @Test
    void parse() throws MalformedURLException {
        assertThat(parser.parse(URI.create("http://localhost:123"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123");
        assertThat(parser.parse(URI.create("http://localhost:123/.well-known"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known/"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known/did.json"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known/did.json/"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known/did.json/asdf"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh:.well-known:did.json:asdf");
        assertThat(parser.parse(URI.create("http://localhost:123/asdf/gh/.well-known/did.json/asdf/"), DEFAULT_CHARSET)).isEqualTo("did:web:localhost%3A123:asdf:gh:.well-known:did.json:asdf");

    }
}