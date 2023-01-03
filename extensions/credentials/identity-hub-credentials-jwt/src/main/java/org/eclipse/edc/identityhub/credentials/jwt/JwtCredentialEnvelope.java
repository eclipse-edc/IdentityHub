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

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Optional;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer.VERIFIABLE_CREDENTIALS_KEY;

public class JwtCredentialEnvelope implements CredentialEnvelope {
    public static final String DATA_FORMAT = "application/vc+jwt";

    private final SignedJWT jwtVerifiableCredentials;

    public JwtCredentialEnvelope(SignedJWT jwtVerifiableCredentials) {
        this.jwtVerifiableCredentials = jwtVerifiableCredentials;
    }

    @Override
    public String format() {
        return DATA_FORMAT;
    }

    @Override
    public Result<VerifiableCredential> toVerifiableCredential(ObjectMapper mapper) {

        try {
            var vcClaim = Optional.ofNullable(jwtVerifiableCredentials.getJWTClaimsSet().getClaim(VERIFIABLE_CREDENTIALS_KEY))
                    .orElseThrow(() -> new EdcException("Missing `vc` claim in signed JWT"));
            return Result.success(mapper.convertValue(vcClaim, VerifiableCredential.class));
        } catch (ParseException e) {
            return Result.failure(e.getMessage());
        }

    }

    public SignedJWT getJwtVerifiableCredentials() {
        return jwtVerifiableCredentials;
    }
}
