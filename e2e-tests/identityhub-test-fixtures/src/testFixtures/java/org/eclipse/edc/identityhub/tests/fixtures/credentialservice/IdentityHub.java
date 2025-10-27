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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

/**
 * Test fixture with Credential Service capabilities.
 */
public class IdentityHub extends AbstractIdentityHub {

    private LazySupplier<Endpoint> credentialsEndpoint;
    private TransactionContext transactionContext;
    private HolderCredentialRequestStore credentialRequestStore;

    public static IdentityHub forContext(ComponentRuntimeContext ctx) {
        return IdentityHub.Builder.newInstance()
                .forContext(ctx)
                .build();
    }

    public Service createServiceEndpoint(String participantContextId) {
        var credentialServiceEndpoint = format("%s/%s", credentialsEndpoint.get().getUrl(), storageApiBasePath(participantContextId));
        return new Service("credential-service-id", "CredentialService", credentialServiceEndpoint);
    }

    private @NotNull String storageApiBasePath(String participantContextId) {
        return "v1/participants/%s".formatted(base64Encode(participantContextId));
    }

    public void waitForCredentialIssuer(String requestId, String participantContext) {
        waitForCredentialIssuer(requestId, participantContext, HolderRequestState.ISSUED);
    }

    public void waitForCredentialIssuer(String requestId, String participantContext, HolderRequestState state) {
        await().pollInterval(interval)
                .atMost(timeout)
                .untilAsserted(() -> assertThat(getCredentialRequestForParticipant(participantContext)).hasSize(1)
                        .allSatisfy(t -> {
                            assertThat(t.getState()).isEqualTo(state.code());
                            assertThat(t.getHolderPid()).isEqualTo(requestId);
                        }));
    }

    public Endpoint getCredentialsEndpoint() {
        return credentialsEndpoint.get();
    }


    public Collection<HolderCredentialRequest> getCredentialRequestForParticipant(String participantContextId, String holderPid) {
        var query = queryByParticipantContextId(participantContextId);
        if (holderPid != null) {
            query.filter(new Criterion("id", "=", holderPid));
        }
        return credentialRequestStore
                .query(query.build());
    }

    public Collection<HolderCredentialRequest> getCredentialRequestForParticipant(String participantContextId) {
        return getCredentialRequestForParticipant(participantContextId, null);
    }

    public void storeHolderRequest(HolderCredentialRequest request) {
        transactionContext.execute(() -> {
            credentialRequestStore.save(request);
        });
    }

    public static class Builder extends AbstractIdentityHub.Builder<IdentityHub, Builder> {
        public Builder() {
            super(new IdentityHub());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder forContext(ComponentRuntimeContext ctx) {
            return super.forContext(ctx)
                    .credentialsEndpoint(ctx.getEndpoint(CREDENTIALS))
                    .transactionContext(ctx.getService(TransactionContext.class))
                    .credentialRequestStore(ctx.getService(HolderCredentialRequestStore.class));
        }

        public Builder credentialsEndpoint(LazySupplier<URI> credentialsEndpoint) {
            this.instance.credentialsEndpoint = new LazySupplier<>(() -> new Endpoint(credentialsEndpoint.get(), Map.of()));
            return self();
        }

        public Builder transactionContext(TransactionContext transactionContext) {
            this.instance.transactionContext = transactionContext;
            return self();
        }

        public Builder credentialRequestStore(HolderCredentialRequestStore credentialRequestStore) {
            this.instance.credentialRequestStore = credentialRequestStore;
            return self();
        }

        @Override
        public IdentityHub build() {
            Objects.requireNonNull(instance.credentialRequestStore, "credentialRequestStore");
            Objects.requireNonNull(instance.transactionContext, "transactionContext");
            Objects.requireNonNull(instance.credentialsEndpoint, "credentialsEndpoint");
            return super.build();
        }
    }
}
