/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.fixtures.issuerservice;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUANCE_API;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUERADMIN;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

/**
 * Test fixture with Issuer Service capabilities.
 */
public class IssuerService extends AbstractIdentityHub {

    protected LazySupplier<Endpoint> adminEndpoint;
    protected LazySupplier<Endpoint> issuerApiEndpoint;

    protected HolderService holderService;
    protected AttestationDefinitionService attestationDefinitionService;
    protected CredentialDefinitionService credentialDefinitionService;


    private IssuerService() {
    }

    public static IssuerService forContext(ComponentRuntimeContext ctx) {
        return IssuerService.Builder.newInstance()
                .forContext(ctx)
                .build();
    }

    public Endpoint getAdminEndpoint() {
        return adminEndpoint.get();
    }

    public Endpoint getIssuerApiEndpoint() {
        return issuerApiEndpoint.get();
    }

    public void createAttestationDefinition(AttestationDefinition definition) {
        attestationDefinitionService.createAttestation(definition)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

    public void createCredentialDefinition(CredentialDefinition definition) {
        credentialDefinitionService.createCredentialDefinition(definition)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

    public Service createServiceEndpoint(String participantContextId) {
        var issuerEndpoint = format("%s/%s", issuerApiEndpoint.get().getUrl(), issuanceBasePath(participantContextId));
        return new Service("issuer-id", "IssuerService", issuerEndpoint);
    }

    public void createHolder(String participantContextId, String holderId, String holderDid, String holderName) {
        var holder = Holder.Builder.newInstance()
                .holderId(holderId)
                .did(holderDid)
                .holderName(holderName)
                .participantContextId(participantContextId)
                .build();

        holderService.createHolder(holder)
                .orElseThrow((f) -> new RuntimeException(f.getFailureDetail()));

    }

    public List<IssuanceProcess> getIssuanceProcessesForParticipant(String participantContextId, String holderPid) {
        var query = ParticipantResource.queryByParticipantContextId(participantContextId);
        if (holderPid != null) {
            query.filter(new org.eclipse.edc.spi.query.Criterion("holderPid", "=", holderPid));
        }
        return getService(IssuanceProcessStore.class).query(query.build())
                .toList();
    }

    public List<IssuanceProcess> getIssuanceProcessesForParticipant(String participantContextId) {
        return getIssuanceProcessesForParticipant(participantContextId, null);
    }

    private @NotNull String issuanceBasePath(String participantContextId) {
        return "v1alpha/participants/%s".formatted(base64Encode(participantContextId));
    }

    public static class Builder extends AbstractIdentityHub.Builder<IssuerService, Builder> {
        public Builder() {
            super(new IssuerService());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder forContext(ComponentRuntimeContext ctx) {
            return super.forContext(ctx)
                    .holderService(ctx.getService(HolderService.class))
                    .attestationDefinitionService(ctx.getService(AttestationDefinitionService.class))
                    .credentialDefinitionService(ctx.getService(CredentialDefinitionService.class))
                    .issuerAdminEndpoint(ctx.getEndpoint(ISSUERADMIN))
                    .issuerApiEndpoint(ctx.getEndpoint(ISSUANCE_API));
        }

        public Builder issuerApiEndpoint(LazySupplier<URI> issuerApiEndpoint) {
            instance.issuerApiEndpoint = new LazySupplier<>(() -> new Endpoint(issuerApiEndpoint.get(), Map.of()));
            return this;
        }

        public Builder issuerAdminEndpoint(LazySupplier<URI> issuerAdminEndpoint) {
            instance.adminEndpoint = new LazySupplier<>(() -> new Endpoint(issuerAdminEndpoint.get(), Map.of()));
            return this;
        }

        public Builder holderService(HolderService holderService) {
            instance.holderService = holderService;
            return this;
        }

        public Builder attestationDefinitionService(AttestationDefinitionService service) {
            instance.attestationDefinitionService = service;
            return this;
        }

        public Builder credentialDefinitionService(CredentialDefinitionService service) {
            instance.credentialDefinitionService = service;
            return this;
        }

        @Override
        public IssuerService build() {
            Objects.requireNonNull(instance.issuerApiEndpoint, "issuerApiEndpoint must be set");
            Objects.requireNonNull(instance.holderService, "holderService must be set");
            return super.build();
        }
    }
}
