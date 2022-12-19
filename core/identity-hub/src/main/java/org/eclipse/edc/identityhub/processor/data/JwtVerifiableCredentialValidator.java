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

package org.eclipse.edc.identityhub.processor.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.processor.data.DataValidator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;

import java.util.Optional;

public class JwtVerifiableCredentialValidator implements DataValidator {

    public static final String DATA_FORMAT = "application/vc+jwt";
    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";

    private final ObjectMapper mapper;

    public JwtVerifiableCredentialValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Result<Void> validate(byte[] data) {
        try {
            var jwt = SignedJWT.parse(new String(data));
            var vcClaim = Optional.ofNullable(jwt.getJWTClaimsSet().getClaim(VERIFIABLE_CREDENTIALS_KEY))
                    .orElseThrow(() -> new EdcException("Missing `vc` claim in signed JWT"));
            mapper.convertValue(vcClaim, VerifiableCredential.class);
        } catch (Exception e) {
            return Result.failure("Failed to parse Verifiable Credential: " + e.getMessage());
        }
        return Result.success();
    }


    @Override
    public String dataFormat() {
        return DATA_FORMAT;
    }
}
