/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verification;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * The AccessTokenVerifier interface represents a verifier for Self-Issued JWT tokens. It takes a base64-encoded ID token.
 */
public interface SelfIssuedTokenVerifier {
    /**
     * Performs the verification on a self-issued ID token, asserting the following aspects:
     * <ul>
     *     <li>iss == aud</li>
     *     <li>aud == the Verifiers own DID. In practice, this will be the DID of the participant agent (i.e. the connector)</li>
     *     <li>the token contains an {@code access_token} claim, and that it is also in JWT format</li>
     *     <li>access_token.sub == sub</li>
     *     <li>that the access_token contains >1 scope strings</li>
     * </ul>
     *
     * @param token         The token to be verified. Must be a JWT in base64 encoding.
     * @param participantId The ID of the {@link ParticipantContext} who is supposed to present their credentials
     * @return A {@code Result} containing a {@code List} of scope strings.
     */
    Result<List<String>> verify(String token, String participantId);
}
