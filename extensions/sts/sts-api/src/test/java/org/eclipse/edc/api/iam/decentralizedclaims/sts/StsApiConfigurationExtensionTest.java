/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.api.iam.decentralizedclaims.sts;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.api.iam.decentralizedclaims.sts.StsApiConfigurationExtension.DEFAULT_STS_PATH;
import static org.eclipse.edc.api.iam.decentralizedclaims.sts.StsApiConfigurationExtension.DEFAULT_STS_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class StsApiConfigurationExtensionTest {

    private final PortMappingRegistry portMappingRegistry = mock();
    private final Monitor monitor = mock();
    private final JettyService jettyService = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(PortMappingRegistry.class, portMappingRegistry);
        context.registerService(JettyService.class, jettyService);
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void initialize_shouldConfigureAndRegisterResource(StsApiConfigurationExtension extension) {
        var context = contextWithConfig(ConfigFactory.empty());

        extension.initialize(context);

        verify(portMappingRegistry).register(new PortMapping(IdentityHubApiContext.STS, DEFAULT_STS_PORT, DEFAULT_STS_PATH));
        verifyNoInteractions(jettyService);
    }

    @Test
    void initialize_withHost_shouldBindStsConnectorOnly(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("web.http.sts.host", "127.0.0.1")));

        factory.constructInstance(StsApiConfigurationExtension.class).initialize(context);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ServerConnector>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(jettyService).addConnectorConfigurationCallback(callback.capture());

        var server = new Server();
        var stsConnector = new ServerConnector(server);
        stsConnector.setName(IdentityHubApiContext.STS);
        var defaultConnector = new ServerConnector(server);
        defaultConnector.setName("default");
        callback.getValue().accept(stsConnector);
        callback.getValue().accept(defaultConnector);

        assertThat(stsConnector.getHost()).isEqualTo("127.0.0.1");
        assertThat(defaultConnector.getHost()).isNull();
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, config);
        context.initialize();
        return context;
    }
}
