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
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformer;
import org.eclipse.edc.spi.result.Result;

import java.nio.charset.StandardCharsets;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.DATA_FORMAT;

public class JwtCredentialEnvelopeTransformer implements CredentialEnvelopeTransformer<JwtCredentialEnvelope> {

    private final ObjectMapper mapper;

    public JwtCredentialEnvelopeTransformer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Result<JwtCredentialEnvelope> parse(byte[] data) {
        try {
            var jwt = SignedJWT.parse(new String(data));
            var envelope = new JwtCredentialEnvelope(jwt);
            // verify that the VC can be properly build from the signed JWT.
            envelope.toVerifiableCredential(mapper);
            return Result.success(new JwtCredentialEnvelope(jwt));
        } catch (Exception e) {
            return Result.failure("Failed to parse Verifiable Credential: " + e.getMessage());
        }
    }

    @Override
    public Result<byte[]> serialize(JwtCredentialEnvelope envelope) {
        return Result.success(envelope.getJwt().serialize().getBytes(StandardCharsets.UTF_8));
    }


    @Override
    public String dataFormat() {
        return DATA_FORMAT;
    }
}
