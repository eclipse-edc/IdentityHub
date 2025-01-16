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
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class KeyDescriptorValidatorTest {

    private final KeyDescriptorValidator validator = new KeyDescriptorValidator(new ConsoleMonitor());

    @Test
    void validate_success() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isSucceeded();
    }

    @Test
    void validate_inputNull() {
        assertThat(validator.validate(null)).isFailed()
                .detail().isEqualTo("input was null");
    }

    @Test
    void validate_keyIdNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("alias")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("keyId cannot be null.");
    }

    @Test
    void validate_privateKeyAliasNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("privateKeyAlias cannot be null.");
    }

    @Test
    void validate_allNull() {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("Either the public key is specified (PEM or JWK), or the generator parameters are provided.");
    }

    @Test
    void validate_bothPublicKeyFormats() {
        var descriptor = KeyDescriptor.Builder.newInstance()
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
                .keyId("key-id")
                .privateKeyAlias("test-alias")
                .publicKeyPem("pemstring")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();
        assertThat(validator.validate(descriptor)).isFailed()
                .detail().isEqualTo("Either the public key is specified (PEM or JWK), or the generator params are provided, not both.");
    }

}