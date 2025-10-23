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

package org.eclipse.edc.identityhub.tests.fixtures.allinone;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

public class AllInOneRuntime {

    private final AllInOneExtension extension;

    public AllInOneRuntime(AllInOneExtension extension) {
        this.extension = extension;
    }

    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", extension.didEndpoint.get().getUrl().getHost(), extension.didEndpoint.get().getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

    public Endpoint getAdminEndpoint() {
        return extension.getAdminEndpoint();
    }

    public void createHolder(String participantContextId, String holderId, String holderDid, String holderName) {
        var holder = Holder.Builder.newInstance()
                .holderId(holderId)
                .did(holderDid)
                .holderName(holderName)
                .participantContextId(participantContextId)
                .build();

        getService(HolderService.class).createHolder(holder)
                .orElseThrow((f) -> new RuntimeException(f.getFailureDetail()));

    }

    public CreateParticipantContextResponse createIssuerParticipant(String participantContextId, String did, String keyId) {
        var issuerServiceEndpoint = createIssuerServiceEndpoint(participantContextId);
        var credentialServiceEndpoint = createServiceEndpoint(participantContextId);
        return createParticipant(participantContextId, did, keyId, List.of(), true, issuerServiceEndpoint, credentialServiceEndpoint);
    }

    public CreateParticipantContextResponse createParticipant(String participantContextId, String did, String keyId, List<String> roles, boolean isActive, Service... service) {
        var builder = ParticipantManifest.Builder.newInstance()
                .participantId(participantContextId)
                .active(isActive)
                .roles(roles)
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING, KeyPairUsage.CREDENTIAL_SIGNING, KeyPairUsage.TOKEN_SIGNING))
                        .privateKeyAlias(participantContextId + "-alias")
                        .resourceId(participantContextId + "-resource")
                        .keyId(keyId)
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .build());
        Arrays.stream(service).forEach(builder::serviceEndpoint);
        var manifest = builder.build();
        var srv = extension.getRuntime().getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public Endpoint getCredentialsEndpoint() {
        return extension.getCredentialsEndpoint();
    }

    public Endpoint getIdentityEndpoint() {
        return extension.getIdentityEndpoint();
    }

    public List<VerifiableCredentialResource> getCredentialsForParticipant(String participantContextId) {
        return getService(CredentialStore.class)
                .query(queryByParticipantContextId(participantContextId).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream()
                .toList();
    }

    public Collection<HolderCredentialRequest> getCredentialRequestForParticipant(String participantContextId, String holderRequestId) {
        var builder = queryByParticipantContextId(participantContextId)
                .filter(new Criterion("id", "=", holderRequestId));
        return getService(HolderCredentialRequestStore.class)
                .query(builder.build());
    }

    public List<IssuanceProcess> getIssuanceProcessesForParticipant(String participantContextId, String holderRequestId) {
        var query = ParticipantResource.queryByParticipantContextId(participantContextId)
                .filter(new Criterion("holderPid", "=", holderRequestId))
                .build();
        return getService(IssuanceProcessStore.class).query(query)
                .toList();
    }

    public <S> S getService(Class<S> klass) {
        return extension.getRuntime().getService(klass);
    }

    private Service createServiceEndpoint(String participantContextId) {
        var credentialServiceEndpoint = format("%s/%s", extension.credentialsEndpoint.get().getUrl(), storageApiBasePath(participantContextId));
        return new Service("credential-service-id", "CredentialService", credentialServiceEndpoint);
    }

    private Service createIssuerServiceEndpoint(String participantContextId) {
        var issuerEndpoint = format("%s/%s", extension.issuerApiEndpoint.get().getUrl(), issuanceBasePath(participantContextId));
        return new Service("issuer-id", "IssuerService", issuerEndpoint);
    }

    private @NotNull String storageApiBasePath(String participantContextId) {
        return "v1/participants/%s".formatted(base64Encode(participantContextId));
    }

    private @NotNull String issuanceBasePath(String participantContextId) {
        return "v1alpha/participants/%s".formatted(base64Encode(participantContextId));
    }


}
