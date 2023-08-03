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

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_PRESENTATION_KEY;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VP_DATA_FORMAT;

public class JwtPresentationEnvelope implements CredentialEnvelope {

    private final SignedJWT jwt;

    public JwtPresentationEnvelope(SignedJWT jwt) {
        this.jwt = jwt;
    }

    @Override
    public String format() {
        return VP_DATA_FORMAT;
    }

    @Override
    public Result<List<VerifiableCredential>> toVerifiableCredentials(ObjectMapper mapper) {
        try {
            var payload = jwt.getJWTClaimsSet().getClaims();
            var vpObject = payload.get(VERIFIABLE_PRESENTATION_KEY);
            if (vpObject == null) {
                return Result.failure(String.format("Missing `%s` claim", VERIFIABLE_PRESENTATION_KEY));
            }
            var jwtPresentation = mapper.convertValue(vpObject, JwtPresentation.class);
            var verifiableCredentials = jwtPresentation.getSignedCredentials().stream().map(jwtVC -> {
                try {
                    return new JwtCredentialEnvelope(SignedJWT.parse(jwtVC))
                            .toVerifiableCredentials(mapper)
                            .getContent();
                } catch (ParseException e) {
                    throw new EdcException("Presentation's verifiableCredential value should be a JWT");
                }
            }).flatMap(Collection::stream).toList();

            return Result.success(verifiableCredentials);
        } catch (Exception e) {
            return Result.failure(Objects.requireNonNullElseGet(e.getMessage(), e::toString));
        }
    }

    public Result<List<SignedJWT>> toJwtVerifiableCredentials(ObjectMapper mapper) {
        try {
            var payload = jwt.getJWTClaimsSet().getClaims();
            var vpObject = payload.get(VERIFIABLE_PRESENTATION_KEY);
            if (vpObject == null) {
                return Result.failure(String.format("Missing `%s` claim", VERIFIABLE_PRESENTATION_KEY));
            }
            var jwtPresentation = mapper.convertValue(vpObject, JwtPresentation.class);
            var signedCredentials = jwtPresentation.getSignedCredentials().stream().map(jwtVC -> {
                try {
                    return SignedJWT.parse(jwtVC);
                } catch (ParseException e) {
                    throw new EdcException("Presentation's verifiableCredential value should be a JWT");
                }
            }).toList();

            return Result.success(signedCredentials);
        } catch (Exception e) {
            return Result.failure(Objects.requireNonNullElseGet(e.getMessage(), e::toString));
        }
    }

    public SignedJWT getJwt() {
        return jwt;
    }
}
