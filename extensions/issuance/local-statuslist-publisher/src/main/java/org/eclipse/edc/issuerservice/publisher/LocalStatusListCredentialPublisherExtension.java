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

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.publisher.api.StatusListCredentialController;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.jetbrains.annotations.NotNull;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


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

    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;

    @Configuration
    private StatusListCredentialEndpointConfig config;

    @Setting(description = "Configures endpoint for reaching the StatusList API.", key = "edc.statuslist.callback.address", required = false)
    private String callbackAddress;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(STATUS_LIST, config.port(), config.path()));

        webServer.registerResource(STATUS_LIST, new StatusListCredentialController(store, monitor, () -> typeManager.getMapper(JSON_LD)));
    }

    @Provider(isDefault = true)
    public StatusListCredentialPublisher createInMemoryStatusListCredentialPublisher() {
        return new LocalCredentialPublisher(baseUrl());
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
