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

package org.eclipse.edc.identityhub.api.configuration;

import org.eclipse.edc.identityhub.api.authentication.filter.RoleBasedAccessFeature;
import org.eclipse.edc.identityhub.api.authentication.filter.UserAuthenticationFilter;
import org.eclipse.edc.identityhub.api.authentication.spi.UserResolver;
import org.eclipse.edc.identityhub.api.authorization.AuthorizationServiceImpl;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.identityhub.api.configuration.ManagementApiConfigurationExtension.NAME;

@Extension(value = NAME)
public class ManagementApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Management API Extension";
    private static final String MGMT_CONTEXT_ALIAS = "management";
    private static final String DEFAULT_DID_PATH = "/api/management";
    private static final int DEFAULT_DID_PORT = 8182;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + MGMT_CONTEXT_ALIAS)
            .contextAlias(MGMT_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_DID_PATH)
            .defaultPort(DEFAULT_DID_PORT)
            .useDefaultContext(false)
            .name("IdentityHub Management API")
            .build();
    @Inject
    private WebService webService;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private WebServiceConfigurer configurer;
    @Inject
    private WebServer webServer;
    @Inject
    private Vault vault;

    private ManagementApiConfiguration configuration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var alias = createApiConfig(context).getContextAlias();

        webService.registerResource(alias, new RoleBasedAccessFeature());
        webService.registerResource(alias, new UserAuthenticationFilter(createUserService()));

        // create super-user
        participantContextService.createParticipantContext(ParticipantManifest.Builder.newInstance()
                        .participantId("super-user")
                        .did("did:web:super-user") // doesn't matter, not intended for resolution
                        .active(true)
                        .key(KeyDescriptor.Builder.newInstance()
                                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                                .keyId("super-user-key")
                                .privateKeyAlias("super-user-alias")
                                .build())
                        .roles(List.of("admin"))
                        .build())
                .onSuccess(apiKey -> context.getMonitor().info("Created user 'super-user'. Please take a note . API Key: %s".formatted(apiKey)));
    }

    @Provider
    public AuthorizationService createAuthService() {
        return new AuthorizationServiceImpl();
    }

    @Provider
    public ManagementApiConfiguration createApiConfig(ServiceExtensionContext context) {
        if (configuration == null) {
            configuration = new ManagementApiConfiguration(configurer.configure(context, webServer, SETTINGS));
        }
        return configuration;
    }

    private UserResolver createUserService() {
        return new ParticipantUserResolver(participantContextService, vault);
    }

}
