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
    private EcPrivateKeyWrapper privateKey;
    private EcPublicKeyWrapper publicKey;

    @BeforeEach
    public void setUp() {
        var key = generateEcKey();
        privateKey = new EcPrivateKeyWrapper(key);
        publicKey = new EcPublicKeyWrapper(key);
    }

    @Test
    void buildSignedJwt_success() throws Exception {
        // Arrange
        var issuer = "test-issuer";
        var subject = "test-subject";

        // Act
        var signedJwt = JwtCredentialFactory.buildSignedJwt(CREDENTIAL, issuer, subject, privateKey, OBJECT_MAPPER);

        // Assert
        boolean result = signedJwt.verify(publicKey.verifier());
        assertThat(result).isTrue();

        assertThat(signedJwt.getJWTClaimsSet().getClaims())
                .containsEntry("iss", issuer)
                .containsEntry("sub", subject)
                .extractingByKey(VERIFIABLE_CREDENTIALS_KEY)
                .satisfies(c -> assertThat(OBJECT_MAPPER.convertValue(c, Credential.class))
                        .usingRecursiveComparison()
                        .isEqualTo(CREDENTIAL));
        assertThat(signedJwt.getJWTClaimsSet().getIssueTime()).isEqualTo(CREDENTIAL.getIssuanceDate());
    }
}
