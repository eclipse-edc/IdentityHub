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

package org.eclipse.edc.identityhub.api.configuration;

import org.eclipse.edc.identityhub.api.authentication.spi.User;
import org.eclipse.edc.identityhub.api.authentication.spi.UserService;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.Base64;
import java.util.List;

class ParticipantUserService implements UserService {
    private final ParticipantContextService participantContextService;
    private final Vault vault;

    ParticipantUserService(ParticipantContextService participantContextService, Vault vault) {
        this.participantContextService = participantContextService;
        this.vault = vault;
    }

    @Override
    public User findByCredential(String apiKey) {
        var tokens = apiKey.split("\\.");
        if (tokens.length != 2) {
            throw new AuthenticationFailedException("Invalid API token");
        }
        var principalId = Base64.getDecoder().decode(tokens[1]);
        var user = findByPrincipal(new String(principalId));
        if (user.getCredential().equals(apiKey)) {
            return user;
        }
        throw new AuthenticationFailedException("Invalid API token");

    }

    @Override
    public User findByPrincipal(String principal) {
        return participantContextService.getParticipantContext(principal)
                .map(this::toUser)
                .orElseThrow(f -> new AuthenticationFailedException("Invalid Authentication '%s': %s".formatted(principal, f.getFailureDetail())));
    }

    private User toUser(ParticipantContext participantContext) {
        var credential = vault.resolveSecret(participantContext.getApiTokenAlias());
        var participantId = participantContext.getParticipantId();
        return new User() {
            @Override
            public String getPrincipal() {
                return participantId;
            }

            @Override
            public String getCredential() {
                return credential;
            }

            @Override
            public List<String> getRoles() {
                return participantContext.getRoles();
            }
        };
    }
}
