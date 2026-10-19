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

package org.eclipse.edc.identityhub.api.verifiablecredential.validation;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ParticipantManifestValidatorTest {

    private final ParticipantManifestValidator validator = new ParticipantManifestValidator(new ConsoleMonitor());

    @NotNull
    private static ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .serviceEndpoint(new Service("id", "type", "foobar"))
                .active(true)
                .did("did:web:test-did")
                .participantContextId("test-id")
                .key(createKeyDescriptor().build());
    }

    @NotNull
    private static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .keyId("key-id")
                .privateKeyAlias("alias")
                .publicKeyJwk(Map.of("foo", "bar"));
    }

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
        assertThat(validator.validate(createManifest().keys(null).build())).isFailed()
                .detail().isEqualTo("key descriptor cannot be null.");

        assertThat(validator.validate(createManifest().keys(Set.of()).build())).isFailed()
                .detail().isEqualTo("key descriptor cannot be empty.");
    }

    @Test
    void validate_keyDescriptorInvalid() {
        var manifest = createManifest().key(createKeyDescriptor().publicKeyPem("pemstring").build()).build();

        assertThat(validator.validate(manifest)).isFailed()
                .detail().startsWith("key descriptor is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\n"})
    @NullAndEmptySource
    void validate_didInvalid(String did) {
        var manifest = createManifest().did(did).build();
        assertThat(validator.validate(manifest)).isFailed()
                .detail().isEqualTo("DID cannot be null or empty.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:web:example.com", "did:web:example.com%3A8443:user:alice", "did:example:123", "did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2doK"})
    void validate_didValid(String did) {
        var manifest = createManifest().did(did).build();
        assertThat(validator.validate(manifest)).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "did:web", "did:web:", "did:web::foo", "did:web:example.com:", "did:web:example.com::user",
            "did:web:example.com/user", "did:web:example.com:user#key-1", "did:web:example.com:user?query", "did:WEB:example.com",
            "did:example:", "did:example:%zz", "did:web:exa mple.com"})
    void validate_didMalformed(String did) {
        var manifest = createManifest().did(did).build();
        assertThat(validator.validate(manifest)).isFailed()
                .detail().isEqualTo("DID '%s' is not a valid DID.".formatted(did));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\n"})
    @NullAndEmptySource
    void validate_participantContextIdNull(String participantId) {
        var manifest = createManifest().participantContextId(participantId).build();
        assertThat(validator.validate(manifest)).isFailed()
                .detail().isEqualTo("participantContextId cannot be null or empty.");
    }
}