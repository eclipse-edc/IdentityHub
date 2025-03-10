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

package org.eclipse.edc.identityhub.tests.fixtures.issuerservice;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHubRuntime;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

public class IssuerRuntime extends AbstractIdentityHubRuntime<IssuerExtension> {

    public IssuerRuntime(IssuerExtension extension) {
        super(extension);
    }

    public Service createServiceEndpoint(String participantContextId) {
        var issuerEndpoint = format("%s/%s", extension.issuerApiEndpoint.get().getUrl(), issuanceBasePath(participantContextId));
        return new Service("issuer-id", "IssuerService", issuerEndpoint);
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

    public List<IssuanceProcess> getIssuanceProcessesForParticipant(String participantContextId) {
        var query = ParticipantResource.queryByParticipantContextId(participantContextId).build();
        return getService(IssuanceProcessStore.class).query(query)
                .toList();
    }

    private @NotNull String issuanceBasePath(String participantContextId) {
        return "v1alpha/participants/%s".formatted(base64Encode(participantContextId));
    }
}
