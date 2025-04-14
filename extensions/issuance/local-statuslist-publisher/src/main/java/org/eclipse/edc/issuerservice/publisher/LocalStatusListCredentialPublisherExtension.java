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

package org.eclipse.edc.issuerservice.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.publisher.api.StatusListCredentialController;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.jetbrains.annotations.NotNull;

import static java.util.Optional.ofNullable;


@Extension(value = LocalStatusListCredentialPublisherExtension.NAME)
public class LocalStatusListCredentialPublisherExtension implements ServiceExtension {
    public static final String NAME = "IssuerService Default Services Extension";
    private static final String STATUS_LIST = "statuslist";
    @Inject
    private CredentialStore store;

    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private Hostname hostname;

    @Inject
    private WebService webServer;

    @Configuration
    private StatusListCredentialEndpointConfig config;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Setting(description = "Configures endpoint for reaching the StatusList API in the form \"<hostname:protocol.port/protocol.path>\"", key = "edc.statuslist.callback.address", required = false)
    private String callbackAddress;

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(STATUS_LIST, config.port(), config.path()));

        webServer.registerResource(STATUS_LIST, new StatusListCredentialController(store, context.getMonitor(), new ObjectMapper()));
    }

    @Provider(isDefault = true)
    public StatusListCredentialPublisher createInMemoryStatusListCredentialPublisher() {
        return new LocalCredentialPublisher(store, baseUrl(), transactionContext);
    }

    private @NotNull String baseUrl() {
        return ofNullable(callbackAddress).orElse("http://%s:%s%s".formatted(hostname.get(), config.port(), config.path()));
    }

    @Settings
    record StatusListCredentialEndpointConfig(
            @Setting(key = "web.http.statuslist.port", defaultValue = "9999", description = "Port of the status list credential web endpoint")
            int port,
            @Setting(key = "web.http.statuslist.path", defaultValue = "/statuslist", description = "Port of the status list credential web endpoint")
            String path
    ) {
    }
}
