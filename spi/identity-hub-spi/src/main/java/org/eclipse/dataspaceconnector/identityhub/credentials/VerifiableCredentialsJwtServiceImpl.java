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

package org.eclipse.dataspaceconnector.identityhub.credentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

public class VerifiableCredentialsJwtServiceImpl implements VerifiableCredentialsJwtService {
    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    private ObjectMapper objectMapper;

    public VerifiableCredentialsJwtServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Map.Entry<String, Object>> extractCredential(SignedJWT jwt) {
        try {
            var payload = jwt.getPayload().toJSONObject();
            var vcObject = payload.get(VERIFIABLE_CREDENTIALS_KEY);
            if (vcObject == null) {
                return Result.failure(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
            }
            var verifiableCredential = objectMapper.convertValue(vcObject, VerifiableCredential.class);

            return Result.success(new AbstractMap.SimpleEntry<>(verifiableCredential.getId(), payload));
        } catch (RuntimeException e) {
            return Result.failure(Objects.requireNonNullElseGet(e.getMessage(), () -> e.toString()));
        }
    }
}
