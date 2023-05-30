/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier.jwt;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifierRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.DATA_FORMAT;

/**
 * Extension to provide verifier for IdentityHub Verifiable Credentials in JWT format.
 */
@Provides(JwtCredentialsVerifier.class)
public class JwtCredentialsVerifierExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private CredentialEnvelopeVerifierRegistry verifierRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return "JWT Credentials Verifier";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jwtVerifier = new DidJwtCredentialsVerifier(didPublicKeyResolver, monitor);
        context.registerService(JwtCredentialsVerifier.class, jwtVerifier);
        verifierRegistry.register(DATA_FORMAT, new JwtCredentialEnvelopeVerifier(jwtVerifier, typeManager.getMapper()));
    }
}
