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

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTestContext {
    public static final String SUPER_USER = "super-user";

    protected final EmbeddedRuntime runtime;

    protected AbstractTestContext(EmbeddedRuntime runtime) {
        this.runtime = runtime;
    }

    public String createParticipant(String participantContextId) {
        return createParticipant(participantContextId, List.of());
    }

    public String createParticipant(String participantContextId, String did, Service service) {
        return createParticipant(participantContextId, did, did + "#key", List.of(), true, service);
    }

    public String createSuperUser() {
        return createParticipant(SUPER_USER, List.of(ServicePrincipal.ROLE_ADMIN));
    }

    public String createParticipant(String participantContextId, List<String> roles, boolean isActive) {
        return createParticipant(participantContextId, "did:web:" + participantContextId, participantContextId + "-key", roles, isActive, createServiceEndpoint(participantContextId));
    }


    public String createParticipant(String participantContextId, String did, String keyId, List<String> roles, boolean isActive, Service service) {
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
                        .build())
                .build();
        var srv = runtime.getService(ParticipantContextService.class);
        return srv.createParticipantContext(manifest)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .apiKey();
    }

    public String createParticipant(String participantContextId, List<String> roles) {
        return createParticipant(participantContextId, roles, true);
    }

    public Optional<VerifiableCredentialResource> getCredential(String credentialId) {
        return runtime.getService(CredentialStore.class)
                .query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream().findFirst();
    }

    public List<VerifiableCredentialResource> getCredentialsForParticipant(String participantContextId) {
        return runtime.getService(CredentialStore.class)
                .query(ParticipantResource.queryByParticipantContextId(participantContextId).build())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream()
                .toList();
    }

    protected Service createServiceEndpoint(String participantContextId) {
        return new Service("test-service-id", "test-type", "http://foo.bar.com");
    }

}
