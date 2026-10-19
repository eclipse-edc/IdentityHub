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
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.keypairs;

import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Map;

/**
 * Checks public keys given as JWK. The public key of a key pair is published in the participant's DID document, so a JWK
 * that carries private key material, e.g. a full key pair that was pasted by mistake, must never be stored as such.
 */
public final class PublicKeyJwk {

    private PublicKeyJwk() {
    }

    /**
     * Parses a JWK that is supplied as the public key of a key pair.
     *
     * @return the JWK, or a failure if it is malformed, contains private parameters, or is a symmetric key.
     */
    public static Result<JWK> parse(Map<String, Object> jwk) {
        JWK parsed;
        try {
            parsed = JWK.parse(jwk);
        } catch (ParseException | RuntimeException e) {
            return Result.failure("The public key JWK is invalid: %s".formatted(e.getMessage()));
        }
        // symmetric (oct) keys are always private
        if (parsed.isPrivate()) {
            return Result.failure("The public key JWK must not contain private key material, or be a symmetric key.");
        }
        return Result.success(parsed);
    }

    /**
     * Reduces a JWK to its public parameters.
     *
     * @return the public JWK, or a failure if the key has no public part, i.e. is a symmetric key.
     */
    public static Result<JWK> publicPart(JWK jwk) {
        var publicJwk = jwk.toPublicJWK();
        return publicJwk == null
                ? Result.failure("The JWK of type '%s' does not have a public key.".formatted(jwk.getKeyType()))
                : Result.success(publicJwk);
    }
}
