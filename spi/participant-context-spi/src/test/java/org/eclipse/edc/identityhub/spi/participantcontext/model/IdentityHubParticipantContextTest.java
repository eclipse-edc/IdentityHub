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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityHubParticipantContextTest {

    @Test
    void verifyCreateTimestamp() {
        var context = IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .apiTokenAlias("foo-token")
                .did("did:example:123")
                .build();

        assertThat(context.getCreatedAt()).isNotZero().isLessThanOrEqualTo(Instant.now().toEpochMilli());

        var context2 = IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .apiTokenAlias("foo-token")
                .createdAt(42)
                .did("did:example:123")
                .build();

        assertThat(context2.getCreatedAt()).isEqualTo(42);
    }

    @Test
    void verifyLastModifiedTimestamp() {
        var context = IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .apiTokenAlias("foo-token")
                .did("did:example:123")
                .build();

        assertThat(context.getLastModified()).isNotZero().isEqualTo(context.getCreatedAt());

        var context2 = IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .apiTokenAlias("foo-token")
                .lastModified(42)
                .did("did:example:123")
                .build();

        assertThat(context2.getLastModified()).isEqualTo(42);
    }

    @Test
    void verifyState() {
        var context = IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("test-id")
                .apiTokenAlias("foo-token")
                .did("did:example:123")
                .state(ParticipantContextState.CREATED);

        assertThat(context.build().getState()).isEqualTo(ParticipantContextState.CREATED.code());
        assertThat(context.state(ParticipantContextState.ACTIVATED).build().getState()).isEqualTo(ParticipantContextState.ACTIVATED.code());
        assertThat(context.state(ParticipantContextState.DEACTIVATED).build().getState()).isEqualTo(ParticipantContextState.DEACTIVATED.code());

    }

}