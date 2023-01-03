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

package org.eclipse.edc.identityhub.verifier;

import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistry;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifierRegistry;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifierRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

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


    private CredentialEnvelopeVerifierRegistry credentialEnvelopeVerifierRegistry;

    @Inject
    private CredentialEnvelopeTransformerRegistry credentialEnvelopeTransformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        credentialEnvelopeVerifierRegistry = new CredentialEnvelopeVerifierRegistryImpl();
    }

    @Override
    public String name() {
        return "Credentials Verifier";
    }

    @Provider
    public CredentialsVerifier createCredentialsVerifier() {
        var client = new IdentityHubClientImpl(httpClient, typeManager.getMapper(), monitor, credentialEnvelopeTransformerRegistry);
        return new IdentityHubCredentialsVerifier(client, monitor, credentialEnvelopeVerifierRegistry);
    }


    @Provider
    public CredentialEnvelopeVerifierRegistry credentialVerifierRegistry() {
        return credentialEnvelopeVerifierRegistry;
    }

}
