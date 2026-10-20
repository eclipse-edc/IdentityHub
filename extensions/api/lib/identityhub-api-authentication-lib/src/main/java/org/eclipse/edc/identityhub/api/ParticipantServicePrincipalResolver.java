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

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.api.authentication.filter.ServicePrincipalAuthenticationFilter;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipalResolver;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.security.MessageDigest;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For the Identity API of the IdentityHub, a {@link ServicePrincipal} is represented by a {@link IdentityHubParticipantContext}.
 * However, the request filter chain ({@link ServicePrincipalAuthenticationFilter} etc.) do not need to know about that;
 * they only know about {@link ServicePrincipal} and {@link ServicePrincipalResolver}. Thus, this implementation acts as
 * a bridge. Other authentication backends like Apache Shiro would call this a _realm_.
 */
class ParticipantServicePrincipalResolver implements ServicePrincipalResolver {
    private final IdentityHubParticipantContextService participantContextService;
    private final Vault vault;
    private final Monitor monitor;

    ParticipantServicePrincipalResolver(IdentityHubParticipantContextService participantContextService, Vault vault, Monitor monitor) {
        this.participantContextService = participantContextService;
        this.vault = vault;
        this.monitor = monitor;
    }

    /**
     * Resolves a {@link ServicePrincipal} based on their credential (= API key). This cannot be done directly, since the API key is stored in a {@link Vault}. However, the
     * API key encodes the user's principal, so we first need to decode the token, and then resolve based on the principal.
     *
     * @param credential The user's credential (= API key)
     * @return The user that owns the credential
     * @throws AuthenticationFailedException if the credential has an invalid structure, or the User cannot be resolved.
     */
    @Override
    public ServicePrincipal findByCredential(String credential) {
        var tokens = credential.split("\\.");
        if (tokens.length != 2) {
            throw new AuthenticationFailedException("Invalid API token");
        }
        byte[] principalId;
        try {
            principalId = Base64.getDecoder().decode(tokens[0]);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException("Invalid API token");
        }
        var user = findByPrincipal(new String(principalId));
        var storedCredential = user.getCredential();
        if (storedCredential != null && MessageDigest.isEqual(storedCredential.getBytes(UTF_8), credential.getBytes(UTF_8))) {
            return user;
        }
        throw new AuthenticationFailedException("Invalid API token");
    }

    private ServicePrincipal findByPrincipal(String principal) {
        return participantContextService.rejectIfDeactivated(principal)
                .map(this::toUser)
                .orElseThrow(f -> {
                    // answered like a wrong API key, so that the participant's existence cannot be told from the response
                    monitor.debug("API key authentication failed: %s".formatted(f.getFailureDetail()));
                    return new AuthenticationFailedException("Invalid API token");
                });
    }

    private ServicePrincipal toUser(IdentityHubParticipantContext participantContext) {
        var credential = vault.resolveSecret(participantContext.getParticipantContextId(), participantContext.getApiTokenAlias());
        var participantContextId = participantContext.getParticipantContextId();
        return new ServicePrincipal() {
            @Override
            public String getPrincipal() {
                return participantContextId;
            }

            @Override
            public String getCredential() {
                return credential;
            }

            @Override
            public String getScope() {
                return String.join(" ", participantContext.getScopes());
            }
        };
    }
}
