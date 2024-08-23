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

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.IdentityHubApiContext;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.api.configuration.IdentityApiConfigurationExtension.NAME;

@Extension(value = NAME)
public class IdentityApiConfigurationExtension implements ServiceExtension {


    public static final String NAME = "Identity API Extension";
    private static final String API_VERSION_JSON_FILE = "identity-api-version.json";
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
    @Inject
    private TypeManager typeManager;

    @Inject
    private ApiVersionService apiVersionService;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
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
                    .forEach(vr -> apiVersionService.addRecord(IdentityHubApiContext.IDENTITY, vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
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
