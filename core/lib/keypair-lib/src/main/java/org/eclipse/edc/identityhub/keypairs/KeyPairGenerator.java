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

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Map;

/**
 * Convenience class, that takes an algorithm name and generator parameters and creates a {@link KeyPair}.
 * Supports the following algorithms:
 * <ul>
 *     <li>RSA: parameters may contain a {@code length} entry, defaults to 2048</li>
 *     <li>EC: parameters may contain a {@code curve} entry, defaults to {@code "secp256r1"}. Curves must be given as std names.</li>
 *     <li>EdDSA: parameters may contain a {@code curve} entry, defaults to {@code "Ed25519"}. Only supports Ed25519 and X25519</li>
 * </ul>
 */
public class KeyPairGenerator {

    public static final String ALGORITHM_RSA = "RSA";
    public static final String ALGORITHM_EC = "EC";
    public static final String ALGORITHM_EDDSA = "EDDSA";
    public static final String CURVE_ED25519 = "ed25519";
    public static final String CURVE_X25519 = "x25519";
    public static final List<String> SUPPORTED_ALGORITHMS = List.of(ALGORITHM_EC, ALGORITHM_RSA, ALGORITHM_EDDSA);
    public static final List<String> SUPPORTED_EDDSA_CURVES = List.of(CURVE_ED25519, CURVE_X25519);
    public static final int RSA_DEFAULT_LENGTH = 2048;
    private static final String RSA_PARAM_LENGTH = "length";
    private static final String EC_PARAM_CURVE = "curve";
    private static final String EC_DEFAULT_CURVE = "secp256r1";
    private static final String ALGORITHM_ENTRY = "algorithm";

    /**
     * Generate a Java {@link KeyPair} from an algorithm identifier and generator parameters.
     *
     * @param parameters May contain specific paramters, such as "length" (RSA), or a "curve" (EC and EdDSA). May be empty, not null.
     * @return A {@link KeyPair}, or a failure indicating what went wrong.
     */
    public static Result<KeyPair> generateKeyPair(Map<String, Object> parameters) {
        var algorithm = parameters.get(ALGORITHM_ENTRY).toString();
        if (StringUtils.isNullOrBlank(algorithm)) {
            return generateEdDsa(CURVE_ED25519);
        }
        algorithm = algorithm.toUpperCase();
        if (SUPPORTED_ALGORITHMS.contains(algorithm)) {
            return switch (algorithm) {
                case ALGORITHM_RSA ->
                        generateRsa(Integer.parseInt(parameters.getOrDefault(RSA_PARAM_LENGTH, RSA_DEFAULT_LENGTH).toString()));
                case ALGORITHM_EC -> generateEc(parameters.getOrDefault(EC_PARAM_CURVE, EC_DEFAULT_CURVE).toString());
                case ALGORITHM_EDDSA ->
                        generateEdDsa(parameters.getOrDefault(EC_PARAM_CURVE, CURVE_ED25519).toString());
                default -> Result.failure(notSupportedError(algorithm));
            };
        }
        return Result.failure(notSupportedError(algorithm));
    }

    private static Result<KeyPair> generateEc(String stdName) {
        try {
            var javaGenerator = java.security.KeyPairGenerator.getInstance(ALGORITHM_EC);
            javaGenerator.initialize(new ECGenParameterSpec(stdName));
            return Result.success(javaGenerator.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            return Result.failure("Error generating EC keys: " + e);
        } catch (InvalidAlgorithmParameterException e) {
            return Result.failure("Error generating EC keys: %s is not a valid or supported EC curve std name. Details: %s".formatted(stdName, e.getMessage()));
        }
    }

    private static Result<KeyPair> generateEdDsa(@NotNull String curve) {
        curve = curve.toLowerCase();
        if (SUPPORTED_EDDSA_CURVES.contains(curve)) {
            try {
                var javaGenerator = java.security.KeyPairGenerator.getInstance(curve);
                return Result.success(javaGenerator.generateKeyPair());
            } catch (NoSuchAlgorithmException e) {
                return Result.failure("Error generating EdDSA/Ed25519 keys: " + e);
            }
        }
        return Result.failure("Unsupported EdDSA Curve: %s. Currently only these are supported: %s.".formatted(curve, String.join(",", SUPPORTED_EDDSA_CURVES)));
    }

    private static Result<KeyPair> generateRsa(int length) {
        try {
            var javaGenerator = java.security.KeyPairGenerator.getInstance(ALGORITHM_RSA);
            javaGenerator.initialize(length, new SecureRandom());
            return Result.success(javaGenerator.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            return Result.failure("Error generating RSA keys: " + e);
        }
    }

    private static String notSupportedError(String algorithm) {
        return "Could not generate key pair for algorithm '%s'. Currently only the following algorithms are supported: %s."
                .formatted(algorithm, String.join(",", SUPPORTED_ALGORITHMS));
    }
}
