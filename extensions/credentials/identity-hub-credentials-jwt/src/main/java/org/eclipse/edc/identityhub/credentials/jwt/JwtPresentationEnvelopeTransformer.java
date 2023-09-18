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
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformer;
import org.eclipse.edc.spi.result.Result;

import java.nio.charset.StandardCharsets;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VP_DATA_FORMAT;

public class JwtPresentationEnvelopeTransformer implements CredentialEnvelopeTransformer<JwtPresentationEnvelope> {

    private final ObjectMapper mapper;

    public JwtPresentationEnvelopeTransformer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Result<JwtPresentationEnvelope> parse(byte[] data) {
        try {
            var jwt = SignedJWT.parse(new String(data));
            var envelope = new JwtPresentationEnvelope(jwt);
            envelope.toVerifiableCredentials(mapper);
            return Result.success(envelope);
        } catch (Exception e) {
            return Result.failure("Failed to parse Verifiable Presentation: " + e.getMessage());
        }
    }

    @Override
    public Result<byte[]> serialize(JwtPresentationEnvelope envelope) {
        return Result.success(envelope.getJwt().serialize().getBytes(StandardCharsets.UTF_8));
    }


    @Override
    public String dataFormat() {
        return VP_DATA_FORMAT;
    }
}
