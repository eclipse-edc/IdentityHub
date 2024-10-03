/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.sts.accountservice;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.identityhub.sts.accountservice.RemoteStsAccountServiceExtension.NAME;


@Extension(value = NAME)
public class RemoteStsAccountServiceExtension implements ServiceExtension {
    public static final String DEFAULT_AUTH_HEADER = "x-api-key";
    public static final String NAME = "Remote STS Account Service Extension";
    @Setting(value = "The name of the Auth header to use. Could be 'Authorization', some custom auth header, etc.", defaultValue = DEFAULT_AUTH_HEADER)
    public static final String AUTH_HEADER = "edc.sts.accounts.api.auth.header.name";
    @Setting(value = "The value of the Auth header to use. Currently we only support static values, e.g. tokens etc.")
    public static final String AUTH_HEADER_VALUE = "edc.sts.accounts.api.auth.header.value";

    @Setting(value = "The base URL of the remote STS Accounts API")
    public static final String REMOTE_STS_API_BASE_URL = "edc.sts.account.api.url";

    @Inject
    private EdcHttpClient edcHttpClient;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsAccountService createAccountManager(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("STS-Account");
        monitor.info("This IdentityHub runtime is configured to manage STS Accounts remotely using the STS Accounts API. That means ParticipantContexts and STS Accounts will be synchronized automatically.");
        return new RemoteStsAccountService(getAccountApiBaseUrl(context), edcHttpClient, getAuthHeaderSupplier(context), monitor, typeManager.getMapper());
    }

    private @NotNull Supplier<Map<String, String>> getAuthHeaderSupplier(ServiceExtensionContext context) {
        //obtain the auth header value outside the supplier to make it fail fast
        var authHeaderValue = context.getConfig().getString(AUTH_HEADER_VALUE);
        return () -> Map.of(context.getConfig().getString(AUTH_HEADER, DEFAULT_AUTH_HEADER), authHeaderValue);
    }

    private String getAccountApiBaseUrl(ServiceExtensionContext context) {
        return context.getConfig().getString(REMOTE_STS_API_BASE_URL);
    }

}
