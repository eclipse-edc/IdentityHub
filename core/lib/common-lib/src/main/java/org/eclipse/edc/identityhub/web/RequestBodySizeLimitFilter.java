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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.REQUEST_ENTITY_TOO_LARGE;

/**
 * Rejects requests whose body is larger than a maximum number of bytes with {@code 413 Payload Too Large}. Requests
 * that announce a larger {@code Content-Length} are aborted before the body is read. For all other requests, the
 * entity stream is bounded, so that reading past the limit fails, which also covers chunked requests.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class RequestBodySizeLimitFilter implements ContainerRequestFilter {

    public static final long DEFAULT_MAX_REQUEST_BODY_SIZE = 5 * 1024 * 1024;

    private final long maxBodySize;

    public RequestBodySizeLimitFilter(long maxBodySize) {
        if (maxBodySize <= 0) {
            throw new IllegalArgumentException("The maximum request body size must be positive but was %d".formatted(maxBodySize));
        }
        this.maxBodySize = maxBodySize;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (contentLength(requestContext) > maxBodySize) {
            requestContext.abortWith(tooLarge(maxBodySize));
            return;
        }
        if (requestContext.hasEntity()) {
            requestContext.setEntityStream(new BoundedInputStream(requestContext.getEntityStream(), maxBodySize));
        }
    }

    /**
     * Parses the {@code Content-Length} header as a long. {@link ContainerRequestContext#getLength()} parses it as an
     * int and fails for larger values, so it is not used here.
     *
     * @return the announced length, -1 if absent, or {@link Long#MAX_VALUE} if it is not a valid number
     */
    private static long contentLength(ContainerRequestContext requestContext) {
        var header = requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        if (header == null) {
            return -1;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static Response tooLarge(long maxBodySize) {
        var error = ApiErrorDetail.Builder.newInstance()
                .message("Request body exceeds the maximum size of %d bytes".formatted(maxBodySize))
                .type(REQUEST_ENTITY_TOO_LARGE.getReasonPhrase())
                .build();
        return Response.status(REQUEST_ENTITY_TOO_LARGE)
                .type(MediaType.APPLICATION_JSON)
                .entity(List.of(error))
                .build();
    }

    /**
     * Fails with a {@link WebApplicationException} carrying a 413 response once more than {@code maxBytes} have been
     * read. An unchecked exception is used so that message body readers do not turn it into a 400 or 500.
     */
    private static final class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long count;

        BoundedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            var b = super.read();
            if (b != -1) {
                increment(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            // never read more than one byte past the limit, so that oversized bodies are not buffered
            var allowed = (int) Math.min(len, maxBytes - count + 1);
            var read = super.read(b, off, allowed);
            if (read > 0) {
                increment(read);
            }
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            var skipped = super.skip(Math.min(n, maxBytes - count + 1));
            increment(skipped);
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        private void increment(long n) {
            count += n;
            if (count > maxBytes) {
                throw new WebApplicationException(tooLarge(maxBytes));
            }
        }
    }
}
