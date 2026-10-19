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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.verification.AuthenticationFailures.idTokenVerificationFailed;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthenticationFailuresTest {

    private final Monitor monitor = mock();

    @Test
    void idTokenVerificationFailed_shouldHideFailureDetail() {
        var result = Result.failure("did:web:internal.host could not be resolved: HTTP 500");

        assertThatThrownBy(() -> result.orElseThrow(idTokenVerificationFailed(monitor, "ID token verification failed")))
                .hasMessage("ID token verification failed");
        verify(monitor).debug("ID token verification failed: did:web:internal.host could not be resolved: HTTP 500");
    }

    @Test
    void idTokenVerificationFailed_serviceFailure_shouldReturnSameMessage() {
        var result = ServiceResult.notFound("ParticipantContext with ID 'unknown' does not exist.");

        assertThatThrownBy(() -> result.orElseThrow(idTokenVerificationFailed(monitor, "Participant context 'unknown' could not be resolved")))
                .hasMessage("ID token verification failed");
        verify(monitor).debug("Participant context 'unknown' could not be resolved: ParticipantContext with ID 'unknown' does not exist.");
    }
}
