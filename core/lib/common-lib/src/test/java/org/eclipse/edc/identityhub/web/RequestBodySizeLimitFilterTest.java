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

package org.eclipse.edc.identityhub.web;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestBodySizeLimitFilterTest {

    private static final int LIMIT = 16;
    private final RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(LIMIT);
    private final ContainerRequestContext requestContext = mock();

    @ParameterizedTest
    @ValueSource(strings = { "17", "2147483648", "99999999999999999999", "not-a-number" })
    void contentLengthAboveLimitOrInvalid_shouldAbortWith413(String contentLength) {
        when(requestContext.getHeaderString(CONTENT_LENGTH)).thenReturn(contentLength);

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(413);
        verify(requestContext, never()).getEntityStream();
    }

    @Test
    void bodyWithinLimit_shouldBeReadable() throws Exception {
        var stream = filterStream(null, new byte[LIMIT]);

        assertThat(stream.readAllBytes()).hasSize(LIMIT);
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void unknownLengthAboveLimit_shouldFailWhenRead() {
        var stream = filterStream(null, new byte[LIMIT * 4]);

        assertThatThrownBy(stream::readAllBytes)
                .isInstanceOfSatisfying(WebApplicationException.class, e -> assertThat(e.getResponse().getStatus()).isEqualTo(413));
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void contentLengthUnderstated_shouldFailWhenRead() {
        var stream = filterStream("1", new byte[LIMIT + 1]);

        assertThatThrownBy(stream::readAllBytes)
                .isInstanceOfSatisfying(WebApplicationException.class, e -> assertThat(e.getResponse().getStatus()).isEqualTo(413));
    }

    @Test
    void unknownLengthAboveLimit_singleByteReads_shouldFail() throws Exception {
        var stream = filterStream(null, new byte[LIMIT + 1]);

        for (var i = 0; i < LIMIT; i++) {
            assertThat(stream.read()).isNotEqualTo(-1);
        }
        assertThatThrownBy(stream::read).isInstanceOf(WebApplicationException.class);
    }

    @Test
    void noEntity_shouldPassUntouched() {
        when(requestContext.hasEntity()).thenReturn(false);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
        verify(requestContext, never()).setEntityStream(any());
    }

    @Test
    void nonPositiveLimit_shouldThrow() {
        assertThatThrownBy(() -> new RequestBodySizeLimitFilter(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private InputStream filterStream(String contentLength, byte[] body) {
        when(requestContext.getHeaderString(CONTENT_LENGTH)).thenReturn(contentLength);
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(InputStream.class);
        verify(requestContext).setEntityStream(captor.capture());
        return captor.getValue();
    }
}
