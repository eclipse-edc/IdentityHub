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

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.EdcException;

import java.util.List;
import java.util.Map;

/**
 * IssuerService end to end context used in tests extended with {@link IssuerServiceEndToEndExtension}
 */
public class IssuerServiceEndToEndTestContext {

    public static final String SUPER_USER = "super-user";

    private final EmbeddedRuntime runtime;
    private final IssuerServiceRuntimeConfiguration configuration;

    public IssuerServiceEndToEndTestContext(EmbeddedRuntime runtime, IssuerServiceRuntimeConfiguration configuration) {
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public IssuerServiceRuntimeConfiguration.Endpoint getAdminEndpoint() {
        return configuration.getAdminEndpoint();
    }

    public IssuerServiceRuntimeConfiguration.Endpoint getDcpIssuanceEndpoint() {
        return configuration.getIssuerApiEndpoint();
    }

    public String createParticipantContext(String participantContextId) {
        return createParticipantContext(participantContextId, List.of());
    }

    public String createParticipantContext(String participantContextId, List<String> roles, boolean isActive) {
        var manifest = ParticipantManifest.Builder.newInstance()
                .participantId(participantContextId)
                .active(isActive)
                .roles(roles)
                .serviceEndpoint(new Service("test-service-id", "test-type", "http://foo.bar.com"))
                .did("did:web:" + participantContextId)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(participantContextId + "-alias")
                        .resourceId(participantContextId + "-resource")
                        .keyId(participantContextId + "-key")
                        .keyGeneratorParams(Map.of("algorithm", "EC", "curve", "secp256r1"))
                        .build())
                .build();
        var srv = runtime.getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .apiKey();
    }


    public String createParticipantContext(String participantContextId, List<String> roles) {
        return createParticipantContext(participantContextId, roles, true);
    }

}
