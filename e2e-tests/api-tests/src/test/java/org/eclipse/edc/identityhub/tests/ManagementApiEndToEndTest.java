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
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.List;
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

    protected static ParticipantManifest.Builder createNewParticipant() {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant")
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(createKeyDescriptor().build());
        return manifest;
    }

    @NotNull
    public static KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("another-alias")
                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                .keyId("another-keyid");
    }

    protected String createSuperUser() {
        return createParticipant(SUPER_USER, List.of(ServicePrincipal.ROLE_ADMIN));
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
        return createParticipant(participantId, List.of());
    }

    protected String createTokenFor(String userId) {
        return new ApiTokenGenerator().generate(userId);
    }

    protected <T> T getService(Class<T> type) {
        return RUNTIME.getContext().getService(type);
    }

    protected Collection<KeyPairResource> getKeyPairsForParticipant(String participantId) {
        return getService(KeyPairResourceStore.class).query(ParticipantResource.queryByParticipantId(participantId).build())
                .getContent();
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

    private String createParticipant(String participantId, List<String> roles) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .active(true)
                .roles(roles)
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
}
