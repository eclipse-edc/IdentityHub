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

package org.eclipse.edc.identityhub.api.participantcontext.v1.validation;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ParticipantManifestValidatorTest {

    private final ParticipantManifestValidator validator = new ParticipantManifestValidator();

    @Test
    void validate_success() {
        var manifest = createManifest().build();
        assertThat(validator.validate(manifest)).isSucceeded();
    }

    @Test
    void validate_inputNull() {
        assertThat(validator.validate(null)).isFailed()
                .detail().isEqualTo("input was null.");
    }

    @Test
    void validate_keyDescriptorNull() {
        var manifest = createManifest().key(null).build();
        assertThat(validator.validate(manifest)).isFailed()
                .detail().isEqualTo("key descriptor cannot be null.");
    }

    @Test
    void validate_keyDescriptorInvalid() {
        var manifest = createManifest().key(createKeyDescriptor().publicKeyPem("pemstring").build()).build();

        assertThat(validator.validate(manifest)).isFailed()
                .detail().startsWith("key descriptor is invalid");
    }

    @Test
    void validate_participantIdNull() {
        var manifest = createManifest().participantId(null).build();
        assertThat(validator.validate(manifest)).isFailed()
                .detail().isEqualTo("participantId cannot be null.");
    }

    @NotNull
    private static ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .serviceEndpoint(new Service("id", "type", "foobar"))
                .autoPublish(true)
                .active(true)
                .participantId("test-id")
                .key(createKeyDescriptor().build());
    }

    @NotNull
    private static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("alias")
                .publicKeyJwk(Map.of("foo", "bar"));
    }
}