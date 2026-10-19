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

package org.eclipse.edc.identityhub.verification;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.function.Function;

/**
 * Maps failures that occur while authenticating a DCP request to the response its caller receives.
 */
public final class AuthenticationFailures {

    public static final String ID_TOKEN_VERIFICATION_FAILED = "ID token verification failed";

    private AuthenticationFailures() {
    }

    /**
     * Returns a mapper that turns a failure into an {@link AuthenticationFailedException} with the fixed message
     * {@value #ID_TOKEN_VERIFICATION_FAILED}, and logs the failure detail at debug level. Every reason a request is not
     * authenticated, such as an unknown participant context or a token that does not verify, is answered the same
     * way, so that callers cannot tell them apart. The detail may also describe the outcome of resolving the sender's
     * DID, which is not for the caller to see either.
     *
     * @param monitor     the monitor the failure detail is logged to
     * @param description what failed, prepended to the failure detail in the log
     */
    public static <F extends Failure> Function<F, AuthenticationFailedException> idTokenVerificationFailed(Monitor monitor, String description) {
        return failure -> {
            monitor.debug("%s: %s".formatted(description, failure.getFailureDetail()));
            return new AuthenticationFailedException(ID_TOKEN_VERIFICATION_FAILED);
        };
    }
}
