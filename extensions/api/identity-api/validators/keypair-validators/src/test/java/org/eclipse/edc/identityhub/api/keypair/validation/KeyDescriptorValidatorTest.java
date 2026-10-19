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

package org.eclipse.edc.identityhub.api.keypair.validation;

import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class KeyDescriptorValidatorTest {

    // the Ed25519 key of RFC 8037, Appendix A
    private static final Map<String, Object> PUBLIC_JWK = Map.of("kty", "OKP", "crv", "Ed25519", "x", "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo");
    private final KeyDescriptorValidator validator = new KeyDescriptorValidator(new ConsoleMonitor());

    @Test
    void validate_success() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isSucceeded();
    }

    @Test
    void validate_nullUsage() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .usage(null)
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("usage must be specified");
    }

    @Test
    void validate_emptyUsage() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .usage(Set.of())
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("usage must be specified");
    }

    @Test
    void validate_inputNull() {
        assertThat(validator.validate(null)).isFailed()
                .detail().isEqualTo("input was null");
    }

    @Test
    void validate_keyIdNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("keyId cannot be null.");
    }

    @Test
    void validate_privateKeyAliasNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("privateKeyAlias cannot be null.");
    }

    @Test
    void validate_allNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("Either the public key is specified (PEM or JWK), or the generator parameters are provided.");
    }

    @Test
    void validate_bothPublicKeyFormats() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyJwk(Map.of("foo", "bar"))
                .publicKeyPem("pemstring")
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("The public key must either be provided in PEM or in JWK format, not both.");
    }

    @Test
    void validate_publicKeyJwkAndGeneratorParams() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyJwk(Map.of("foo", "bar"))
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("Either the public key is specified (PEM or JWK), or the generator params are provided, not both.");
    }


    @Test
    void validate_publicKeyPemAndGeneratorParams() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyPem("pemstring")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("Either the public key is specified (PEM or JWK), or the generator params are provided, not both.");
    }

    @Test
    void validate_publicKeyJwk() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyJwk(PUBLIC_JWK)
                .build();
        assertThat(validator.validate(descriptor)).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidPublicKeyJwks.class)
    void validate_publicKeyJwkInvalid(Map<String, Object> jwk, String expectedDetail) {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyJwk(jwk)
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().contains(expectedDetail);
    }

    private static class InvalidPublicKeyJwks implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    // a full key pair, pasted by mistake
                    Arguments.of(Map.of("kty", "OKP", "crv", "Ed25519", "x", "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo", "d", "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A"), "must not contain private key material"),
                    Arguments.of(Map.of("kty", "oct", "k", "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A"), "must not contain private key material"),
                    Arguments.of(Map.of("foo", "bar"), "The public key JWK is invalid")
            );
        }
    }
}
