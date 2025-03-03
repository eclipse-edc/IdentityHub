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
import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractTestContext;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

/**
 * IssuerService end to end context used in tests extended with {@link IssuerServiceEndToEndExtension}
 */
public class IssuerServiceEndToEndTestContext extends AbstractTestContext {


    private final IssuerServiceRuntimeConfiguration configuration;

    public IssuerServiceEndToEndTestContext(EmbeddedRuntime runtime, IssuerServiceRuntimeConfiguration configuration) {
        super(runtime);
        this.configuration = configuration;
    }


    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public Endpoint getAdminEndpoint() {
        return configuration.getAdminEndpoint();
    }

    public Endpoint getDcpIssuanceEndpoint() {
        return configuration.getIssuerApiEndpoint();
    }

    public String didFor(String participantContextId) {
        return configuration.didFor(participantContextId);
    }

    public Service createServiceEndpoint(String participantContextId) {
        var issuerEndpoint = format("%s/%s", configuration.getIssuerApiEndpoint().getUrl(), issuanceBasePath(participantContextId));
        return new Service("issuer-id", "IssuerService", issuerEndpoint);
    }


    public List<IssuanceProcess> getIssuanceProcessesForParticipant(String participantContextId) {
        var query = ParticipantResource.queryByParticipantContextId(participantContextId).build();
        return runtime.getService(IssuanceProcessStore.class).query(query)
                .toList();
    }

    private @NotNull String issuanceBasePath(String participantContextId) {
        return "v1alpha/participants/%s".formatted(base64Encode(participantContextId));
    }
}
