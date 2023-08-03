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
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VC_DATA_FORMAT;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_CREDENTIALS_KEY;

public class JwtCredentialEnvelope implements CredentialEnvelope {

    private final SignedJWT jwt;

    public JwtCredentialEnvelope(SignedJWT jwt) {
        this.jwt = jwt;
    }

    @Override
    public String format() {
        return VC_DATA_FORMAT;
    }

    @Override
    public Result<List<VerifiableCredential>> toVerifiableCredentials(ObjectMapper mapper) {
        try {
            var payload = jwt.getJWTClaimsSet().getClaims();
            var vcObject = payload.get(VERIFIABLE_CREDENTIALS_KEY);
            if (vcObject == null) {
                return Result.failure(String.format("Missing `%s` claim", VERIFIABLE_CREDENTIALS_KEY));
            }
            var credential = mapper.convertValue(vcObject, Credential.class);
            // JWT Verifiable Credentials do not have embedded proof.
            return Result.success(List.of(new VerifiableCredential(credential, null)));
        } catch (Exception e) {
            return Result.failure(Objects.requireNonNullElseGet(e.getMessage(), e::toString));
        }
    }

    public SignedJWT getJwt() {
        return jwt;
    }
}
