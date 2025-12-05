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

import io.restassured.http.Header;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.tests.fixtures.common.Oauth2TokenProvider;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub.SUPER_USER;

public class TestFunctions {

    /**
     * Create a token-based authorization header for the given participant context id. The participant context is created
     * if it does not yet exist
     */
    public static Header authorizeTokenBased(String participantContextId, IdentityHub identityHub) {
        if (SUPER_USER.equals(participantContextId)) {
            return new Header("x-api-key", identityHub.createSuperUser().apiKey());
        }
        return new Header("x-api-key", identityHub.createParticipant(participantContextId).apiKey());
    }

    /**
     * Create an OAuth2 authorization header for the given participant context id. The participant context is created if it
     * does not yet exist.
     */
    public static Header authorizeOauth2(String participantContextId, IdentityHub identityHub, Oauth2TokenProvider tokenProvider) {
        if (SUPER_USER.equals(participantContextId)) {
            identityHub.createSuperUser();
        } else {
            identityHub.createParticipant(participantContextId);
        }
        return new Header("Authorization", "Bearer " + tokenProvider.createToken(participantContextId));

    }

    public static ParticipantManifest.Builder createNewParticipant() {
        return ParticipantManifest.Builder.newInstance()
                .participantContextId("another-participant")
                .active(false)
                .did("did:web:another:participant:" + UUID.randomUUID())
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(createKeyDescriptor().build());
    }

    public static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
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
