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

package org.eclipse.dataspaceconnector.identityhub.verifier;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

/**
 * Extension to provide verifier for IdentityHub Verifiable Credentials in JWT format.
 */
public class JwtCredentialsVerifierExtension implements ServiceExtension {
    @Inject
    private Monitor monitor;
    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Override
    public String name() {
        return "JWT Credentials Verifier";
    }

    @Provider
    public JwtCredentialsVerifier createJwtVerifier() {
        return new DidJwtCredentialsVerifier(didPublicKeyResolver, monitor);
    }
}
