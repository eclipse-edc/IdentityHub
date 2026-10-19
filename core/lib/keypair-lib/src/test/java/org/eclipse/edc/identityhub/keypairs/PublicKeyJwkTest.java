/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.keypairs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class PublicKeyJwkTest {

    @Test
    void parse_publicKey() throws JOSEException {
        var key = new ECKeyGenerator(Curve.P_256).generate().toPublicJWK();

        assertThat(PublicKeyJwk.parse(key.toJSONObject())).isSucceeded()
                .isEqualTo(key);
    }

    @Test
    void parse_privateKey() throws JOSEException {
        var key = new RSAKeyGenerator(2048).generate();

        assertThat(PublicKeyJwk.parse(key.toJSONObject())).isFailed()
                .detail().contains("must not contain private key material");
    }

    @Test
    void parse_symmetricKey() throws JOSEException {
        var key = new OctetSequenceKeyGenerator(256).generate();

        assertThat(PublicKeyJwk.parse(key.toJSONObject())).isFailed()
                .detail().contains("must not contain private key material");
    }

    @Test
    void parse_malformed() {
        assertThat(PublicKeyJwk.parse(Map.of("kty", "EC", "crv", "P-256"))).isFailed()
                .detail().contains("The public key JWK is invalid");
    }

    @Test
    void publicPart_privateKey() throws JOSEException {
        var key = new ECKeyGenerator(Curve.P_256).generate();

        assertThat(PublicKeyJwk.publicPart(key)).isSucceeded()
                .satisfies(jwk -> assertThat(jwk.isPrivate()).isFalse())
                .isEqualTo(key.toPublicJWK());
    }

    @Test
    void publicPart_symmetricKey() throws JOSEException {
        assertThat(PublicKeyJwk.publicPart(new OctetSequenceKeyGenerator(256).generate())).isFailed()
                .detail().contains("does not have a public key");
    }
}
