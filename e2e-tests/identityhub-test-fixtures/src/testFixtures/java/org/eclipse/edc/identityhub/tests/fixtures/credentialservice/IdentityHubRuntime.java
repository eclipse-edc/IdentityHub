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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHubRuntime;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

public class IdentityHubRuntime extends AbstractIdentityHubRuntime<IdentityHubExtension> {

    public IdentityHubRuntime(IdentityHubExtension extension) {
        super(extension);
    }

    public Service createServiceEndpoint(String participantContextId) {
        var credentialServiceEndpoint = format("%s/%s", extension.credentialsEndpoint.get().getUrl(), storageApiBasePath(participantContextId));
        return new Service("credential-service-id", "CredentialService", credentialServiceEndpoint);
    }

    private @NotNull String storageApiBasePath(String participantContextId) {
        return "v1/participants/%s".formatted(base64Encode(participantContextId));
    }


    public void waitForCredentialIssuer(String requestId, String participantContext) {
        waitForCredentialIssuer(requestId, participantContext, HolderRequestState.ISSUED);
    }

    public void waitForCredentialIssuer(String requestId, String participantContext, HolderRequestState state) {
        await().pollInterval(extension.getInterval())
                .atMost(extension.getTimeout())
                .untilAsserted(() -> assertThat(getCredentialRequestForParticipant(participantContext)).hasSize(1)
                        .allSatisfy(t -> {
                            assertThat(t.getState()).isEqualTo(state.code());
                            assertThat(t.getHolderPid()).isEqualTo(requestId);
                        }));
    }

    public Endpoint getCredentialsEndpoint() {
        return extension.getCredentialsEndpoint();
    }

    public Collection<HolderCredentialRequest> getCredentialRequestForParticipant(String participantContextId) {
        return getService(HolderCredentialRequestStore.class)
                .query(queryByParticipantContextId(participantContextId).build());
    }

    public void storeHolderRequest(HolderCredentialRequest request) {
        getService(TransactionContext.class).execute(() -> {
            getService(HolderCredentialRequestStore.class)
                    .save(request);
        });
    }

}
