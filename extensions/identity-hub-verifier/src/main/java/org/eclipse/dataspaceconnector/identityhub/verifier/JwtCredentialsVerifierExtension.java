/*
 *  Copyright (c) 2021 Microsoft Corporation
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
 * Extension to provide verification of DID JWTs
 */
public class JwtCredentialsVerifierExtension implements ServiceExtension {

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private Monitor monitor;

    @Provider(isDefault = true)
    public JwtCredentialsVerifier createJwtVerifier() {
        return new DidJwtCredentialsVerifier(didPublicKeyResolver, monitor);
    }
}
