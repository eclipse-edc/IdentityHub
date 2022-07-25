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

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import static java.lang.String.format;

/**
 * Extension to provide verification of IdentityHub Verifiable Credentials.
 */
public class CredentialsVerifierExtension implements ServiceExtension {

    @EdcSetting
    private static final String HUB_URL_SETTING = "edc.identity.hub.url";

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;

    @Inject
    private JwtCredentialsVerifier jwtCredentialsVerifier;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initialized Identity Hub DID extension");
    }

    @Provider
    public CredentialsVerifier createCredentialsVerifier(ServiceExtensionContext context) {
        var hubUrl = context.getSetting(HUB_URL_SETTING, null);
        if (hubUrl == null) {
            throw new EdcException(format("Mandatory setting '(%s)' missing", HUB_URL_SETTING));
        }

        var client = new IdentityHubClientImpl(httpClient, typeManager.getMapper(), monitor);
        var verifiableCredentialsJwtUtils = new VerifiableCredentialsJwtUtils(typeManager.getMapper());
        return new IdentityHubCredentialsVerifier(client, monitor, jwtCredentialsVerifier, verifiableCredentialsJwtUtils);
    }
}
