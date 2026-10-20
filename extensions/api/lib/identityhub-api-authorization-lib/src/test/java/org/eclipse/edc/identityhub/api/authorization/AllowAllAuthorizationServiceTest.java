/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.authorization;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AllowAllAuthorizationServiceTest {

    private final Monitor monitor = mock();

    @Test
    void constructor_logsWarning() {
        new AllowAllAuthorizationService(monitor, "Test API");

        verify(monitor).warning(contains("the Test API does not check resource ownership"));
    }

    @Test
    void authorize_allowsAll() {
        var service = new AllowAllAuthorizationService(monitor, "Test API");

        var result = service.authorize(mock(SecurityContext.class), "owner", "resource-id", ParticipantResource.class);

        assertThat(result.succeeded()).isTrue();
    }
}
