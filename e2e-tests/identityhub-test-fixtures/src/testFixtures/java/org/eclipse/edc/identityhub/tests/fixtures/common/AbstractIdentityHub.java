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
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.IDENTITY;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.IH_DID;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.STS;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

/**
 * Base utility class for identity components such as Identity Hub, Issuer service.
 * This class provides common methods to create participants, key pairs, store and retrieve credentials, DIDs, etc.
 */
public abstract class AbstractIdentityHub {

    public static final String SUPER_USER = "super-user";

    protected LazySupplier<Endpoint> identityEndpoint;
    protected LazySupplier<Endpoint> stsEndpoint;
    protected LazySupplier<Endpoint> didEndpoint;

    protected ParticipantContextService participantContextService;
    protected ParticipantContextStore participantContextStore;
    protected DidDocumentService didDocumentService;
    protected CredentialStore credentialStore;
    protected KeyPairService keyPairService;
    protected KeyPairResourceStore keyPairResourceStore;
    protected TransactionContext transactionContext;
    protected Function<Class<?>, ?> serviceLocator;
    protected Vault vault;

    protected Duration interval = Duration.ofSeconds(1);
    protected Duration timeout = Duration.ofSeconds(60);

    public Endpoint getStsEndpoint() {
        return stsEndpoint.get();
    }

    public Endpoint getIdentityEndpoint() {
        return identityEndpoint.get();
    }

    public abstract Service createServiceEndpoint(String participantContextId);

    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", didEndpoint.get().getUrl().getHost(), didEndpoint.get().getUrl().getPort());
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

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId, List<Service> services) {
        return createParticipant(participantContextId, did, keyId, List.of(), true, services);
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

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId, List<String> roles, boolean isActive, Service service) {
        return createParticipant(participantContextId, did, keyId, roles, isActive, List.of(service));
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId, List<String> roles, boolean isActive, List<Service> services) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(participantContextId)
                .active(isActive)
                .roles(roles)
                .serviceEndpoints(new HashSet<>(services))
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(participantContextId + "-alias")
                        .resourceId(participantContextId + "-resource")
                        .keyId(keyId)
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .usage(Set.of(KeyPairUsage.values()))
                        .build())
                .build();
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public IdentityHubParticipantContext getParticipant(String participantContextId) {
        return participantContextService
                .getParticipantContext(participantContextId)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public Collection<KeyPairResource> getKeyPairsForParticipant(String participantContextId) {
        return keyPairResourceStore.query(queryByParticipantContextId(participantContextId).build())
                .getContent();
    }

    public Collection<DidDocument> getDidForParticipant(String participantContextId) {
        return didDocumentService.queryDocuments(queryByParticipantContextId(participantContextId).build()).getContent();
    }

    public DidResource getDidResourceForParticipant(String did) {
        return didDocumentService.findById(did);
    }

    public List<VerifiableCredentialResource> getCredentialsForParticipant(String participantContextId) {
        return credentialStore
                .query(queryByParticipantContextId(participantContextId).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream()
                .toList();
    }
    
    public String storeParticipant(IdentityHubParticipantContext pc) {

        var token = createTokenFor(pc.getParticipantContextId());
        vault.storeSecret(pc.getParticipantContextId(), pc.getApiTokenAlias(), token);
        participantContextStore.create(pc).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        return token;
    }

    public String createTokenFor(String userId) {
        var secureRandom = new SecureRandom();
        var array = new byte[64];
        secureRandom.nextBytes(array);
        var enc = Base64.getEncoder();
        return enc.encodeToString(userId.getBytes()) + "." + enc.encodeToString(array);
    }

    public KeyDescriptor createKeyPair(String participantContextId) {
        var descriptor = createKeyDescriptor(participantContextId).build();
        return createKeyPair(participantContextId, descriptor);
    }

    public KeyDescriptor createKeyPair(String participantContextId, KeyDescriptor descriptor) {
        keyPairService.addKeyPair(participantContextId, descriptor, true)
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
        credentialStore.create(resource).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return resource.getId();
    }

    public Optional<VerifiableCredentialResource> getCredential(String credentialId) {
        return credentialStore
                .query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream().findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) serviceLocator.apply(serviceClass);
    }

    public abstract static class Builder<P extends AbstractIdentityHub, B extends Builder<P, B>> {
        protected P instance;

        protected Builder(P instance) {
            this.instance = instance;
        }

        public B forContext(ComponentRuntimeContext ctx) {
            return stsEndpoint(ctx.getEndpoint(STS))
                    .credentialStore(ctx.getService(CredentialStore.class))
                    .identityEndpoint(ctx.getEndpoint(IDENTITY))
                    .didEndpoint(ctx.getEndpoint(IH_DID))
                    .participantContextService(ctx.getService(ParticipantContextService.class))
                    .participantContextStore(ctx.getService(ParticipantContextStore.class))
                    .didDocumentService(ctx.getService(DidDocumentService.class))
                    .keyPairService(ctx.getService(KeyPairService.class))
                    .keyPairResourceStore(ctx.getService(KeyPairResourceStore.class))
                    .vault(ctx.getService(Vault.class))
                    .transactionContext(ctx.getService(TransactionContext.class))
                    .serviceLocator(ctx::getService);
        }

        public B stsEndpoint(LazySupplier<URI> stsEndpoint) {
            this.instance.stsEndpoint = new LazySupplier<>(() -> new Endpoint(stsEndpoint.get(), Map.of()));
            return self();
        }

        public B identityEndpoint(LazySupplier<URI> identityEndpoint) {
            this.instance.identityEndpoint = new LazySupplier<>(() -> new Endpoint(identityEndpoint.get(), Map.of()));
            return self();
        }

        public B didEndpoint(LazySupplier<URI> didEndpoint) {
            this.instance.didEndpoint = new LazySupplier<>(() -> new Endpoint(didEndpoint.get(), Map.of()));
            return self();
        }

        public B timeout(Duration timeout) {
            this.instance.timeout = timeout;
            return self();
        }

        public B interval(Duration interval) {
            this.instance.interval = interval;
            return self();
        }

        public B participantContextService(ParticipantContextService participantContextService) {
            this.instance.participantContextService = participantContextService;
            return self();
        }

        public B participantContextStore(ParticipantContextStore participantContextStore) {
            this.instance.participantContextStore = participantContextStore;
            return self();
        }

        public B didDocumentService(DidDocumentService didDocumentService) {
            this.instance.didDocumentService = didDocumentService;
            return self();
        }

        public B credentialStore(CredentialStore credentialStore) {
            this.instance.credentialStore = credentialStore;
            return self();
        }

        public B keyPairService(KeyPairService keyPairService) {
            this.instance.keyPairService = keyPairService;
            return self();
        }

        public B keyPairResourceStore(KeyPairResourceStore keyPairResourceStore) {
            this.instance.keyPairResourceStore = keyPairResourceStore;
            return self();
        }

        public B vault(Vault vault) {
            this.instance.vault = vault;
            return self();
        }

        public B transactionContext(TransactionContext transactionContext) {
            this.instance.transactionContext = transactionContext;
            return self();
        }

        public B serviceLocator(Function<Class<?>, ?> serviceLocator) {
            this.instance.serviceLocator = serviceLocator;
            return self();
        }

        public P build() {
            Objects.requireNonNull(instance.stsEndpoint, "stsEndpoint");
            Objects.requireNonNull(instance.identityEndpoint, "identityEndpoint");
            Objects.requireNonNull(instance.didEndpoint, "didEndpoint");
            Objects.requireNonNull(instance.participantContextService, "participantContextService");
            Objects.requireNonNull(instance.participantContextStore, "participantContextStore");
            Objects.requireNonNull(instance.didDocumentService, "didDocumentService");
            Objects.requireNonNull(instance.credentialStore, "credentialStore");
            Objects.requireNonNull(instance.keyPairService, "keyPairService");
            Objects.requireNonNull(instance.keyPairResourceStore, "keyPairResourceStore");
            Objects.requireNonNull(instance.vault, "vault");
            Objects.requireNonNull(instance.serviceLocator, "serviceLocator");
            return instance;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
    }
}
