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
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ParticipantServicePrincipalResolverTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String API_KEY = Base64.getEncoder().encodeToString(PARTICIPANT_CONTEXT_ID.getBytes()) + ".secret-part";

    private final IdentityHubParticipantContextService participantContextService = mock();
    private final Vault vault = mock();
    private final ParticipantServicePrincipalResolver resolver = new ParticipantServicePrincipalResolver(participantContextService, vault, mock());

    @BeforeEach
    void setUp() {
        when(participantContextService.getParticipantContext(PARTICIPANT_CONTEXT_ID)).thenReturn(ServiceResult.success(createContext()));
    }

    @Test
    void findByCredential() {
        when(vault.resolveSecret(PARTICIPANT_CONTEXT_ID, "test-alias")).thenReturn(API_KEY);

        var principal = resolver.findByCredential(API_KEY);

        assertThat(principal.getPrincipal()).isEqualTo(PARTICIPANT_CONTEXT_ID);
        assertThat(principal.getCredential()).isEqualTo(API_KEY);
    }

    @Test
    void findByCredential_whenCredentialDoesNotMatch_shouldThrow() {
        when(vault.resolveSecret(PARTICIPANT_CONTEXT_ID, "test-alias")).thenReturn(API_KEY);

        var otherKey = Base64.getEncoder().encodeToString(PARTICIPANT_CONTEXT_ID.getBytes()) + ".other-secret";
        assertThatThrownBy(() -> resolver.findByCredential(otherKey)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid API token");
    }

    @Test
    void findByCredential_whenParticipantNotFound_shouldThrowSameErrorAsForWrongKey() {
        var unknownId = "unknown-participant";
        when(participantContextService.getParticipantContext(unknownId)).thenReturn(ServiceResult.notFound("ParticipantContext with ID '%s' does not exist.".formatted(unknownId)));

        var apiKey = Base64.getEncoder().encodeToString(unknownId.getBytes()) + ".secret-part";
        assertThatThrownBy(() -> resolver.findByCredential(apiKey)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid API token");
        verifyNoInteractions(vault);
    }

    @Test
    void findByCredential_whenApiKeyNotInVault_shouldThrow() {
        when(vault.resolveSecret(PARTICIPANT_CONTEXT_ID, "test-alias")).thenReturn(null);

        assertThatThrownBy(() -> resolver.findByCredential(API_KEY)).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void findByCredential_whenPrincipalNotBase64_shouldThrow() {
        assertThatThrownBy(() -> resolver.findByCredential("not-base64!.secret-part")).isInstanceOf(AuthenticationFailedException.class);
        verifyNoInteractions(vault);
    }

    @Test
    void findByCredential_whenInvalidStructure_shouldThrow() {
        assertThatThrownBy(() -> resolver.findByCredential("no-separator")).isInstanceOf(AuthenticationFailedException.class);
        verifyNoInteractions(vault);
    }

    private IdentityHubParticipantContext createContext() {
        return IdentityHubParticipantContext.Builder.newInstance()
                .id(PARTICIPANT_CONTEXT_ID)
                .did("did:web:test")
                .state(ParticipantContextState.ACTIVATED)
                .apiTokenAlias("test-alias")
                .build();
    }
}
