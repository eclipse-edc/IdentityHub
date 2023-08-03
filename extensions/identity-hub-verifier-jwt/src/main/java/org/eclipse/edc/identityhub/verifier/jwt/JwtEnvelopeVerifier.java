/*
 *  Copyright (c) 2023 GAIA-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       GAIA-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier.jwt;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of an abstract Verifier for an envelope with a JWT
 */
public abstract class JwtEnvelopeVerifier {

    private final JwtCredentialsVerifier jwtCredentialsVerifier;

    protected JwtEnvelopeVerifier(JwtCredentialsVerifier jwtCredentialsVerifier) {
        this.jwtCredentialsVerifier = jwtCredentialsVerifier;
    }

    @NotNull Result<SignedJWT> verifyJwtClaims(SignedJWT jwt, DidDocument didDocument) {
        var result = jwtCredentialsVerifier.verifyClaims(jwt, didDocument.getId());
        return result.succeeded() ? Result.success(jwt) : Result.failure(result.getFailureMessages());
    }

    @NotNull Result<SignedJWT> verifySignature(SignedJWT jwt) {
        var result = jwtCredentialsVerifier.isSignedByIssuer(jwt);
        return result.succeeded() ? Result.success(jwt) : Result.failure(result.getFailureMessages());
    }
}
