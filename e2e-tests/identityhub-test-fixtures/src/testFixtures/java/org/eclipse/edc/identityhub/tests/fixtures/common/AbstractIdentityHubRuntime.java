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

package org.eclipse.edc.identityhub.tests.fixtures.common;

import com.nimbusds.jose.jwk.Curve;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;

public abstract class AbstractIdentityHubRuntime<T extends AbstractIdentityHubExtension> {

    public static final String SUPER_USER = "super-user";

    protected final T extension;

    public AbstractIdentityHubRuntime(T extension) {
        this.extension = extension;
    }

    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", extension.didEndpoint.get().getUrl().getHost(), extension.didEndpoint.get().getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

    public CreateParticipantContextResponse createSuperUser() {
        return createParticipant(SUPER_USER, List.of(ServicePrincipal.ROLE_ADMIN));
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId) {
        return createParticipant(participantContextId, "did:web:" + participantContextId);
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did) {
        return createParticipant(participantContextId, did, createServiceEndpoint(participantContextId));
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId) {
        return createParticipant(participantContextId, did, keyId, List.of(), true, createServiceEndpoint(participantContextId));
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, List<String> roles) {
        return createParticipant(participantContextId, roles, true);
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, List<String> roles, boolean isActive) {
        return createParticipant(participantContextId, "did:web:" + participantContextId, participantContextId + "-key", roles, isActive, createServiceEndpoint(participantContextId));
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, Service service) {
        return createParticipant(participantContextId, did, participantContextId + "-key", List.of(), true, service);
    }


    public abstract Service createServiceEndpoint(String participantContextId);

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId, List<String> roles, boolean isActive, Service service) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantContextId)
                .active(isActive)
                .roles(roles)
                .serviceEndpoint(service)
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(participantContextId + "-alias")
                        .resourceId(participantContextId + "-resource")
                        .keyId(keyId)
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .usage(Set.of(KeyPairUsage.values()))
                        .build())
                .build();
        var srv = extension.getRuntime().getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public ParticipantContext getParticipant(String participantContextId) {
        return getService(ParticipantContextService.class)
                .getParticipantContext(participantContextId)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public Collection<KeyPairResource> getKeyPairsForParticipant(String participantContextId) {
        return getService(KeyPairResourceStore.class).query(queryByParticipantContextId(participantContextId).build())
                .getContent();
    }

    public Collection<DidDocument> getDidForParticipant(String participantContextId) {
        return getService(DidDocumentService.class).queryDocuments(queryByParticipantContextId(participantContextId).build()).getContent();
    }

    public DidResource getDidResourceForParticipant(String did) {
        return getService(DidDocumentService.class).findById(did);
    }

    public List<VerifiableCredentialResource> getCredentialsForParticipant(String participantContextId) {
        return getService(CredentialStore.class)
                .query(queryByParticipantContextId(participantContextId).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream()
                .toList();
    }

    public String storeParticipant(ParticipantContext pc) {
        var store = getService(ParticipantContextStore.class);

        var vault = getService(Vault.class);
        var token = createTokenFor(pc.getParticipantContextId());
        vault.storeSecret(pc.getApiTokenAlias(), token);
        store.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    public String createTokenFor(String userId) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] array = new byte[64];
        secureRandom.nextBytes(array);
        var enc = Base64.getEncoder();
        return enc.encodeToString(userId.getBytes()) + "." + enc.encodeToString(array);
    }

    public KeyDescriptor createKeyPair(String participantContextId) {

        var descriptor = createKeyDescriptor(participantContextId).build();
        return createKeyPair(participantContextId, descriptor);
    }

    public KeyDescriptor createKeyPair(String participantContextId, KeyDescriptor descriptor) {
        var service = getService(KeyPairService.class);
        service.addKeyPair(participantContextId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor;
    }

    public KeyDescriptor.Builder createKeyDescriptor(String participantContextId) {
        var keyId = "key-id-%s".formatted(UUID.randomUUID());
        return KeyDescriptor.Builder.newInstance()
                .keyId(keyId)
                .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                .active(false)
                .resourceId(UUID.randomUUID().toString())
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias("%s-%s-alias".formatted(participantContextId, keyId));
    }

    public String storeCredential(VerifiableCredential credential, String participantContextId) {
        var resource = VerifiableCredentialResource.Builder.newHolder()
                .id(UUID.randomUUID().toString())
                .state(VcStatus.ISSUED)
                .participantContextId(participantContextId)
                .holderId("holderId")
                .issuerId("issuerId")
                .credential(new VerifiableCredentialContainer("rawVc", CredentialFormat.VC1_0_JWT, credential))
                .build();
        getService(CredentialStore.class).create(resource).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return resource.getId();
    }

    public Optional<VerifiableCredentialResource> getCredential(String credentialId) {
        return getService(CredentialStore.class)
                .query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream().findFirst();
    }

    public Endpoint getIdentityEndpoint() {
        return extension.getIdentityEndpoint();
    }

    public <S> S getService(Class<S> klass) {
        return extension.getRuntime().getService(klass);
    }
}


