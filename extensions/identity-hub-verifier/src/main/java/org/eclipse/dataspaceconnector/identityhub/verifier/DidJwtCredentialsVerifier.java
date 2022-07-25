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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.text.ParseException;
import java.util.Set;

/**
 * Verifies verifiable credentials in JWT format with a DID issuer.
 */
class DidJwtCredentialsVerifier implements JwtCredentialsVerifier {

    private final DidPublicKeyResolver didPublicKeyResolver;
    private final Monitor monitor;

    // RFC 7519 Registered (standard) claim
    private static final String ISSUER_CLAIM = "iss";

    DidJwtCredentialsVerifier(DidPublicKeyResolver didPublicKeyResolver, Monitor monitor) {
        this.didPublicKeyResolver = didPublicKeyResolver;
        this.monitor = monitor;
    }

    public boolean isSignedByIssuer(SignedJWT jwt) {
        String issuer;
        try {
            issuer = jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            monitor.warning("Error parsing issuer from JWT", e);
            return false;
        }
        var issuerPublicKey = didPublicKeyResolver.resolvePublicKey(issuer);
        if (issuerPublicKey.failed()) {
            monitor.warning(String.format("Failed finding publicKey of issuer: %s", issuer));
            return false;
        }
        Result<Void> signatureResult = verifySignature(jwt, issuerPublicKey.getContent());
        return signatureResult.succeeded();
    }

    public boolean verifyClaims(SignedJWT jwt, String expectedSubject) {
        JWTClaimsSet jwtClaimsSet;
        try {
            jwtClaimsSet = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            monitor.warning("Error parsing issuer from JWT", e);
            return false;
        }

        // verify claims
        var exactMatchClaims = new JWTClaimsSet.Builder()
                .subject(expectedSubject)
                .build();
        var requiredClaims = Set.of(ISSUER_CLAIM);

        var claimsVerifier = new DefaultJWTClaimsVerifier<>(exactMatchClaims, requiredClaims);
        try {
            claimsVerifier.verify(jwtClaimsSet);
        } catch (BadJWTException e) {
            monitor.warning("Failure verifying JWT token", e);
            return false;
        }

        return true;
    }

    private Result<Void> verifySignature(SignedJWT jwt, PublicKeyWrapper issuerPublicKey) {
        try {
            var verified = jwt.verify(issuerPublicKey.verifier());
            if (!verified) {
                return Result.failure("Invalid signature");
            }
            return Result.success();
        } catch (JOSEException e) {
            return Result.failure("Unable to verify JWT token. " + e.getMessage());
        }
    }
}
