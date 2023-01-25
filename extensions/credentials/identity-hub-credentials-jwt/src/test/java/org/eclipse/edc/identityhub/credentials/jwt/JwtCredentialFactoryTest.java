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
import org.eclipse.edc.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.edc.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_CREDENTIALS_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

class JwtCredentialFactoryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Credential CREDENTIAL = generateCredential();

    private JwtCredentialFactory jwtCredentialFactory;
    private EcPrivateKeyWrapper privateKey;
    private EcPublicKeyWrapper publicKey;

    @BeforeEach
    public void setUp() {
        var key = generateEcKey();
        jwtCredentialFactory = new JwtCredentialFactory(OBJECT_MAPPER);
        privateKey = new EcPrivateKeyWrapper(key);
        publicKey = new EcPublicKeyWrapper(key);
    }

    @Test
    void buildSignedJwt_success() throws Exception {
        var signedJwt = jwtCredentialFactory.buildSignedJwt(CREDENTIAL, privateKey);

        boolean result = signedJwt.verify(publicKey.verifier());
        assertThat(result).isTrue();

        assertThat(signedJwt.getJWTClaimsSet().getClaims())
                .containsEntry("iss", CREDENTIAL.getIssuer())
                .containsEntry("sub", CREDENTIAL.getCredentialSubject().getId())
                .extractingByKey(VERIFIABLE_CREDENTIALS_KEY)
                .satisfies(c -> assertThat(OBJECT_MAPPER.convertValue(c, Credential.class))
                        .usingRecursiveComparison()
                        .isEqualTo(CREDENTIAL));
        assertThat(signedJwt.getJWTClaimsSet().getIssueTime()).isEqualTo(CREDENTIAL.getIssuanceDate());
    }
}
