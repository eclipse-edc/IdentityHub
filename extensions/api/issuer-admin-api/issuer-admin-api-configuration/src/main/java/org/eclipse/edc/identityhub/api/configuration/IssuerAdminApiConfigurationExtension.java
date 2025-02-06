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

package org.eclipse.edc.identityhub.api.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.api.configuration.IssuerAdminApiConfigurationExtension.NAME;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUERADMIN;

@Extension(value = NAME)
public class IssuerAdminApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Issuer Admin API Configuration Extension";
    private static final String API_VERSION_JSON_FILE = "issuer-admin-api-version.json";

    @Configuration
    private IdentityApiConfiguration apiConfiguration;

    @Inject
    private TypeManager typeManager;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private ApiVersionService apiVersionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(ISSUERADMIN, apiConfiguration.port(), apiConfiguration.path()));

        registerVersionInfo(getClass().getClassLoader());
    }

    @Provider(isDefault = true)
    public AuthorizationService authorizationService() {
        return new AllowAllAuthorizationService();
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("issuer-admin-api", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record IdentityApiConfiguration(
            @Setting(key = "web.http." + ISSUERADMIN + ".port", description = "Port for " + ISSUERADMIN + " api context", defaultValue = 15152 + "")
            int port,
            @Setting(key = "web.http." + ISSUERADMIN + ".path", description = "Path for " + ISSUERADMIN + " api context", defaultValue = "/api/issuer")
            String path
    ) {

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
