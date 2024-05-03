/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.keypairs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.AlgorithmParameters;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class KeyPairGeneratorTest {

    @Test
    void generateKeyPair_rsa_defaultLength() {
        var rsaResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "RSA"));
        assertThat(rsaResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .isInstanceOf(RSAPrivateKey.class);

        var key = (RSAPrivateKey) rsaResult.getContent().getPrivate();
        Assertions.assertThat(key.getModulus().bitLength()).isEqualTo(2048); //could theoretically be less if key has 8 leading zeros, but we control the key generation.
    }

    @Test
    void generateKeyPair_rsa_withLength() {
        var rsaResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "RSA", "length", 4096));
        assertThat(rsaResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .isInstanceOf(RSAPrivateKey.class);

        var key = (RSAPrivateKey) rsaResult.getContent().getPrivate();
        Assertions.assertThat(key.getModulus().bitLength()).isEqualTo(4096); //could theoretically be less if key has 8 leading zeros, but we control the key generation.
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, -1, Integer.MAX_VALUE })
    void generateKeyPair_rsa_withInvalidLength(int invalidLength) {
        Assertions.assertThatThrownBy(() -> KeyPairGenerator.generateKeyPair(Map.of("algorithm", "RSA", "length", invalidLength))).isInstanceOf(InvalidParameterException.class);
    }

    @Test
    void generateKeyPair_ec_defaultCurve() throws InvalidParameterSpecException, NoSuchAlgorithmException {
        var ecResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EC"));
        assertThat(ecResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .isInstanceOf(ECPrivateKey.class);

        var key = (ECPrivateKey) ecResult.getContent().getPrivate();
        var algorithmParameters = AlgorithmParameters.getInstance("EC");
        algorithmParameters.init(key.getParams());
        var oid = algorithmParameters.getParameterSpec(ECGenParameterSpec.class).getName();
        Assertions.assertThat(oid).isEqualTo("1.2.840.10045.3.1.7"); // no easy way to get the std name, only the OID
    }

    @ParameterizedTest()
    @ValueSource(strings = { "secp256r1", "secp384r1", "secp521r1", "SECP256R1", "SecP521R1" })
    void generateKeyPair_ec_withCurve(String curve) {
        var ecResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EC", "curve", curve));
        assertThat(ecResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .isInstanceOf(ECPrivateKey.class);
    }

    @ParameterizedTest()
    @ValueSource(strings = { "secp256k1", "foobar" })
    @EmptySource
    void generateKeyPair_ec_withInvalidCurve(String curve) {
        var ecResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EC", "curve", curve));
        assertThat(ecResult).isFailed()
                .detail().contains("not a valid or supported EC curve std name");
    }


    @Test
    void generateKeyPair_edDsa() {
        var edDsaResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EdDSA"));
        assertThat(edDsaResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .satisfies(k -> Assertions.assertThat(k.getClass().getName()).isEqualTo("sun.security.ec.ed.EdDSAPrivateKeyImpl")); // not available at compile time
    }

    @ParameterizedTest
    @ValueSource(strings = { "Ed25519", "X25519", "ed25519", "x25519", "ED25519" })
    void generateKeyPair_edDsa_withValidCurve(String curve) {
        var edDsaResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EdDSA", "curve", curve));
        assertThat(edDsaResult).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .satisfies(k -> Assertions.assertThat(k.getClass().getName()).startsWith("sun.security.ec.")); // not available at compile time
    }

    @ParameterizedTest
    @ValueSource(strings = { "Ed448", "x448", "foobar" })
    void generateKeyPair_edDsa_withInvalidCurve(String invalidCurve) {
        var edDsaResult = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "EdDSA", "curve", invalidCurve));
        assertThat(edDsaResult).isFailed()
                .detail().contains("Unsupported EdDSA Curve: %s. Currently only these are supported: ed25519,x25519.".formatted(invalidCurve.toLowerCase()));
    }

    @Test
    void generateKeyPair_noAlgorithm() {
        var result = KeyPairGenerator.generateKeyPair(Map.of("algorithm", "", "foo", "bar"));
        assertThat(result).isSucceeded()
                .extracting(KeyPair::getPrivate)
                .satisfies(k -> Assertions.assertThat(k.getClass().getName()).isEqualTo("sun.security.ec.ed.EdDSAPrivateKeyImpl")); // not available at compile time
    }

    @Test
    void generateKeyPair_unknownAlgorithm() {
        assertThat(KeyPairGenerator.generateKeyPair(Map.of("algorithm", "foobar"))).isFailed()
                .detail().matches("Could not generate key pair for algorithm '.*'. Currently only the following algorithms are supported: .*.");
    }
}