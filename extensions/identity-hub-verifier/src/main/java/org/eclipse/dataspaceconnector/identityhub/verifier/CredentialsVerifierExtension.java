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

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * Extension to provide verifier for IdentityHub Verifiable Credentials.
 */
public class CredentialsVerifierExtension implements ServiceExtension {

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;

    @Inject
    private JwtCredentialsVerifier jwtCredentialsVerifier;

    @Override
    public String name() {
        return "Credentials Verifier";
    }

    @Provider
    public CredentialsVerifier createCredentialsVerifier() {
        var client = new IdentityHubClientImpl(httpClient, typeManager.getMapper(), monitor);
        var verifiableCredentialsJwtService = new VerifiableCredentialsJwtServiceImpl(typeManager.getMapper(), monitor);
        return new IdentityHubCredentialsVerifier(client, monitor, jwtCredentialsVerifier, verifiableCredentialsJwtService);
    }
}
