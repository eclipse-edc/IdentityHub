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
import org.eclipse.edc.identityhub.tests.fixtures.common.Oauth2TokenProvider;
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
    public static Header authorizeOauth2(String participantContextId, IssuerService issuerService, Oauth2TokenProvider tokenProvider) {
        if (SUPER_USER.equals(participantContextId)) {
            issuerService.createSuperUser();
        } else {
            issuerService.createParticipant(participantContextId);
        }
        return new Header("Authorization", "Bearer " + tokenProvider.createToken(participantContextId));

    }
}
