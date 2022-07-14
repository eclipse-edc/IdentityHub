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

package org.eclipse.dataspaceconnector.identityhub.cli;

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.model.credentials.VerifiableCredential;

import java.util.Map;

public class TestUtils {
    static final Faker FAKER = new Faker();

    private TestUtils() {
    }

    public static VerifiableCredential createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .credentialSubject(Map.of(
                        FAKER.internet().uuid(), FAKER.lorem().word(),
                        FAKER.internet().uuid(), FAKER.lorem().word()))
                .build();
    }

    public static SignedJWT getSignedVerifiableCredential(VerifiableCredential vc) {
        try {
            return JWTUtils.buildSignedJwt(
                    vc,
                    "identity-hub",
                    new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate().toECPrivateKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}