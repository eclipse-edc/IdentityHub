/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.verifier;

import com.nimbusds.jwt.SignedJWT;

/**
 * Verifies verifiable credentials in JWT format.
 */
public interface JwtCredentialsVerifier {

    /**
     * Verifies if a JWT is really signed by the claimed issuer (iss field).
     *
     * @param jwt to be verified.
     * @return if the JWT is signed by the claimed issuer.
     */
    boolean isSignedByIssuer(SignedJWT jwt);

    /**
     * Verifies if a JWT targets the given subject, and checks for the presence of the issuer ("iss") claim. The expiration ("exp") and not-before ("nbf") claims are verified if present as well.
     *
     * @param jwt             to be verified.
     * @param expectedSubject subject claim to verify.
     * @return if the JWT is valid and for the given subject
     */
    boolean verifyClaims(SignedJWT jwt, String expectedSubject);
}
