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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import com.nimbusds.jose.jwk.Curve;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.participantcontext.ApiTokenGenerator;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractTestContext;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

/**
 * Identity Hub end to end context used in tests extended with {@link IdentityHubEndToEndExtension}
 */
public class IdentityHubEndToEndTestContext extends AbstractTestContext {

    public static final String SUPER_USER = "super-user";

    private final IdentityHubRuntimeConfiguration configuration;

    public IdentityHubEndToEndTestContext(EmbeddedRuntime runtime, IdentityHubRuntimeConfiguration configuration) {
        super(runtime);
        this.configuration = configuration;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }


    public VerifiableCredential createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type("test-type")
                .issuanceDate(Instant.now())
                .issuer(new Issuer("did:web:issuer"))
                .credentialSubject(CredentialSubject.Builder.newInstance().id("id").claim("foo", "bar").build())
                .build();
    }

    public String storeCredential(VerifiableCredential credential, String participantContextId) {
        var resource = VerifiableCredentialResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .state(VcStatus.ISSUED)
                .participantContextId(participantContextId)
                .holderId("holderId")
                .issuerId("issuerId")
                .credential(new VerifiableCredentialContainer("rawVc", CredentialFormat.VC1_0_JWT, credential))
                .build();
        runtime.getService(CredentialStore.class).create(resource).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return resource.getId();
    }


    public String createSuperUser() {
        return createParticipant(SUPER_USER, List.of(ServicePrincipal.ROLE_ADMIN));
    }

    public Service createServiceEndpoint(String participantContextId) {
        var credentialServiceEndpoint = format("%s/%s", configuration.getStorageEndpoint().getUrl(), storageApiBasePath(participantContextId));
        return new Service("credential-service-id", "CredentialService", credentialServiceEndpoint);
    }

    public String storeParticipant(ParticipantContext pc) {
        var store = runtime.getService(ParticipantContextStore.class);

        var vault = runtime.getService(Vault.class);
        var token = createTokenFor(pc.getParticipantContextId());
        vault.storeSecret(pc.getApiTokenAlias(), token);
        store.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    public Endpoint getIdentityApiEndpoint() {
        return configuration.getIdentityApiEndpoint();
    }

    public Endpoint getPresentationEndpoint() {
        return configuration.getPresentationEndpoint();
    }

    public Endpoint getStorageEndpoint() {
        return configuration.getStorageEndpoint();
    }

    public Collection<DidDocument> getDidForParticipant(String participantContextId) {
        return runtime.getService(DidDocumentService.class).queryDocuments(QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build()).getContent();
    }

    public Collection<KeyPairResource> getKeyPairsForParticipant(String participantContextId) {
        return runtime.getService(KeyPairResourceStore.class).query(ParticipantResource.queryByParticipantContextId(participantContextId).build())
                .getContent();
    }

    public Collection<HolderCredentialRequest> getCredentialRequestForParticipant(String participantContextId) {
        return runtime.getService(HolderCredentialRequestStore.class)
                .query(ParticipantResource.queryByParticipantContextId(participantContextId).build());
    }

    public KeyDescriptor createKeyPair(String participantContextId) {

        var descriptor = createKeyDescriptor(participantContextId).build();
        return createKeyPair(participantContextId, descriptor);
    }

    public KeyDescriptor createKeyPair(String participantContextId, KeyDescriptor descriptor) {
        var service = runtime.getService(KeyPairService.class);
        service.addKeyPair(participantContextId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor;
    }

    public KeyDescriptor.Builder createKeyDescriptor(String participantContextId) {
        var keyId = "key-id-%s".formatted(UUID.randomUUID());
        return KeyDescriptor.Builder.newInstance()
                .keyId(keyId)
                .active(false)
                .resourceId(UUID.randomUUID().toString())
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias("%s-%s-alias".formatted(participantContextId, keyId));
    }

    public ParticipantManifest.Builder createNewParticipant() {
        return ParticipantManifest.Builder.newInstance()
                .participantId("another-participant")
                .active(false)
                .did("did:web:another:participant:" + UUID.randomUUID())
                .serviceEndpoint(new Service("test-service", "test-service-type", "https://test.com"))
                .key(createKeyDescriptor().build());
    }

    public KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("another-alias")
                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                .keyId("another-keyid");
    }

    public String createTokenFor(String userId) {
        return new ApiTokenGenerator().generate(userId);
    }

    public ParticipantContext getParticipant(String participantContextId) {
        return runtime.getService(ParticipantContextService.class)
                .getParticipantContext(participantContextId)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }


    public DidResource getDidResourceForParticipant(String did) {
        return runtime.getService(DidDocumentService.class).findById(did);
    }

    public String didFor(String participantContextId) {
        return configuration.didFor(participantContextId);
    }

    private @NotNull String storageApiBasePath(String participantContextId) {
        return "v1alpha/participants/%s".formatted(base64Encode(participantContextId));
    }

}
