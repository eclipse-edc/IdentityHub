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

package org.eclipse.edc.identityhub.tests;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identityhub.participantcontext.ApiTokenGenerator;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.Map;

/**
 * Base class for all management API tests
 */
public abstract class ManagementApiEndToEndTest {
    public static final String SUPER_USER = "super-user";
    protected static final IdentityHubRuntimeConfiguration RUNTIME_CONFIGURATION = IdentityHubRuntimeConfiguration.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    @RegisterExtension
    protected static final EdcRuntimeExtension RUNTIME = new EdcRuntimeExtension(":launcher", "identity-hub", RUNTIME_CONFIGURATION.controlPlaneConfiguration());

    protected String getSuperUserApiKey() {
        var vault = RUNTIME.getContext().getService(Vault.class);
        return vault.resolveSecret("super-user-apikey");
    }

    protected String storeParticipant(ParticipantContext pc) {
        var store = RUNTIME.getContext().getService(ParticipantContextStore.class);

        var vault = RUNTIME.getContext().getService(Vault.class);
        var token = createTokenFor(pc.getParticipantId());
        vault.storeSecret(pc.getApiTokenAlias(), token);
        store.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    protected String createParticipant(String participantId) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .active(true)
                .serviceEndpoint(new Service("test-service-id", "test-type", "http://foo.bar.com"))
                .did("did:web:" + participantId)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(participantId + "-alias")
                        .keyId(participantId + "-key")
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .build())
                .build();
        var srv = RUNTIME.getContext().getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    protected String createTokenFor(String userId) {
        return new ApiTokenGenerator().generate(userId);
    }

    protected <T> T getService(Class<T> type) {
        return RUNTIME.getContext().getService(type);
    }

    protected Collection<KeyPairResource> getKeyPairsForParticipant(ParticipantManifest manifest) {
        return getService(KeyPairResourceStore.class).query(QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantId", "=", manifest.getParticipantId()))
                .build()).getContent();
    }

    protected Collection<DidDocument> getDidForParticipant(String participantId) {
        return getService(DidDocumentService.class).queryDocuments(QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantId", "=", participantId))
                .build()).getContent();
    }

    protected ParticipantContext getParticipant(String participantId) {
        return getService(ParticipantContextService.class)
                .getParticipantContext(participantId)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    protected static ParticipantManifest createNewParticipant() {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant")
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias("another-alias")
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId("another-keyid")
                        .build())
                .build();
        return manifest;
    }
}
