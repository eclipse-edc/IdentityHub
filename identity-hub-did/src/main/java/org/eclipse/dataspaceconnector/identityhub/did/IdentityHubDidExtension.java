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

package org.eclipse.dataspaceconnector.identityhub.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import static java.lang.String.format;

/**
 * Extension that should be used to provide verification of IdentityHub Verifiable Credentials.
 */
@Requires({OkHttpClient.class})
@Provides(CredentialsVerifier.class)
public class IdentityHubDidExtension implements ServiceExtension {

    @EdcSetting
    private static final String HUB_URL_SETTING = "edc.identity.hub.url";

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var hubUrl = context.getSetting(HUB_URL_SETTING, null);
        if (hubUrl == null) {
            throw new EdcException(format("Mandatory setting '(%s)' missing", HUB_URL_SETTING));
        }

        var client = new IdentityHubClientImpl(httpClient, new ObjectMapper(), monitor);
        var signatureVerifier = new SignatureVerifier(didPublicKeyResolver, monitor);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(client, monitor, signatureVerifier::isSignedByIssuer);
        context.registerService(CredentialsVerifier.class, credentialsVerifier);

        monitor.info("Initialized Identity Hub DID extension");
    }
}
