/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class TestFunctions {

    public static ParticipantManifest.Builder createNewParticipant() {
        return ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant:" + UUID.randomUUID())
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(createKeyDescriptor().build());
    }

    public static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("another-alias")
                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                .keyId("another-keyid");
    }

    public static VerifiableCredential createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type("test-type")
                .issuanceDate(Instant.now())
                .issuer(new Issuer("did:web:issuer"))
                .credentialSubject(CredentialSubject.Builder.newInstance().id("id").claim("foo", "bar").build())
                .build();
    }
}
