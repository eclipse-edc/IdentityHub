/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.tests.fixtures;

import io.restassured.http.Header;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;

import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub.SUPER_USER;

public class TestFunctions {
    /**
     * Create a token-based authorization header for the given participant context id. The participant context is created
     * if it does not yet exist
     */
    public static Header authorizeTokenBased(String participantContextId, IssuerService issuerService) {
        if (SUPER_USER.equals(participantContextId)) {
            return new Header("x-api-key", issuerService.createSuperUser().apiKey());
        }
        return new Header("x-api-key", issuerService.createParticipant(participantContextId).apiKey());
    }

    /**
     * Create an OAuth2 authorization header for the given participant context id. The participant context is created if it
     * does not yet exist.
     */
    public static Header authorizeOauth2(String participantContextId, IssuerService issuerService, OauthServer server) {
        var role = ParticipantPrincipal.ROLE_PARTICIPANT;
        if (SUPER_USER.equals(participantContextId)) {
            issuerService.createSuperUser();
            role = ParticipantPrincipal.ROLE_ADMIN;
        } else {
            issuerService.createParticipant(participantContextId);
        }
        var scopes = "management-api:read management-api:write identity-api:read identity-api:write issuer-admin-api:read issuer-admin-api:write";

        return new Header("Authorization", "Bearer " + server.createToken(participantContextId, scopes, role));

    }
}
