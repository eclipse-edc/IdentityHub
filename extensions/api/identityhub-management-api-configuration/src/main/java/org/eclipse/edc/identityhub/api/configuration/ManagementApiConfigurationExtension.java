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

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ManagementApiConfiguration;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.api.configuration.ManagementApiConfigurationExtension.NAME;

@Extension(value = NAME)
public class ManagementApiConfigurationExtension implements ServiceExtension {

    @Setting(value = "Explicitly set the initial API key for the Super-User")
    public static final String SUPERUSER_APIKEY_PROPERTY = "edc.ih.api.superuser.key";
    public static final String NAME = "Management API Extension";
    public static final String SUPER_USER_PARTICIPANT_ID = "super-user";
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

    private ManagementApiConfigurationImpl configuration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        // create super-user
        participantContextService.createParticipantContext(ParticipantManifest.Builder.newInstance()
                        .participantId(SUPER_USER_PARTICIPANT_ID)
                        .did("did:web:%s".formatted(SUPER_USER_PARTICIPANT_ID)) // doesn't matter, not intended for resolution
                        .active(true)
                        .key(KeyDescriptor.Builder.newInstance()
                                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                                .keyId("%s-key".formatted(SUPER_USER_PARTICIPANT_ID))
                                .privateKeyAlias("%s-alias".formatted(SUPER_USER_PARTICIPANT_ID))
                                .build())
                        .roles(List.of(ServicePrincipal.ROLE_ADMIN))
                        .build())
                .onSuccess(generatedKey -> {
                    var monitor = context.getMonitor();
                    var apiKey = ofNullable(context.getSetting(SUPERUSER_APIKEY_PROPERTY, null))
                            .map(key -> {
                                if (!key.contains(".")) {
                                    monitor.warning("Super-user key override: this key appears to have an invalid format, you may be unable to access some APIs. It must follow the structure: 'base64(<participantId>).<random-string>'");
                                }
                                participantContextService.getParticipantContext(SUPER_USER_PARTICIPANT_ID)
                                        .onSuccess(pc -> vault.storeSecret(pc.getApiTokenAlias(), key)
                                                .onSuccess(u -> monitor.debug("Super-user key override successful"))
                                                .onFailure(f -> monitor.warning("Error storing API key in vault: %s".formatted(f.getFailureDetail()))))
                                        .onFailure(f -> monitor.warning("Error overriding API key for '%s': %s".formatted(SUPER_USER_PARTICIPANT_ID, f.getFailureDetail())));
                                return key;
                            })
                            .orElse(generatedKey);
                    monitor.info("Created user 'super-user'. Please take note of the API Key: %s".formatted(apiKey));
                })
                .orElseThrow(f -> new EdcException("Error creating Super-User: " + f.getFailureDetail()));
    }


    @Provider
    public ManagementApiConfiguration createApiConfig(ServiceExtensionContext context) {
        if (configuration == null) {
            configuration = new ManagementApiConfigurationImpl(configurer.configure(context, webServer, SETTINGS));
        }
        return configuration;
    }

    @Provider(isDefault = true)
    public AuthorizationService authorizationService() {
        return new AllowAllAuthorizationService();
    }


    private static class AllowAllAuthorizationService implements AuthorizationService {
        @Override
        public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<? extends ParticipantResource> resourceClass) {
            return ServiceResult.success();
        }

        @Override
        public void addLookupFunction(Class<?> resourceClass, Function<String, ParticipantResource> checkFunction) {

        }
    }
}
