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

package org.eclipse.edc.protocols.dcp.credentialrequest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.protocols.dcp.credentialrequest.v1alpha.api.CredentialRequestApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIAL_REQUEST;
import static org.eclipse.edc.protocols.dcp.credentialrequest.CredentialRequestApiExtension.NAME;

@Extension(value = NAME)
public class CredentialRequestApiExtension implements ServiceExtension {
    public static final String NAME = "CredentialRequestApiExtension";

    private static final String API_VERSION_JSON_FILE = "credential-request-api-version.json";

    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private WebService webService;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    @Configuration
    private CredentialRequestApiConfiguration apiConfiguration;

    @Override
    public void initialize(ServiceExtensionContext context) {

        portMappingRegistry.register(new PortMapping(CREDENTIAL_REQUEST, apiConfiguration.port(), apiConfiguration.path()));

        var controller = new CredentialRequestApiController();
        webService.registerResource(CREDENTIAL_REQUEST, controller);

        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file '%s' not found or not readable.".formatted(API_VERSION_JSON_FILE));
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("credential-request", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record CredentialRequestApiConfiguration(
            @Setting(key = "web.http." + CREDENTIAL_REQUEST + ".port", description = "Port for " + CREDENTIAL_REQUEST + " api context", defaultValue = 13132 + "")
            int port,
            @Setting(key = "web.http." + CREDENTIAL_REQUEST + ".path", description = "Path for " + CREDENTIAL_REQUEST + " api context", defaultValue = "/api/issuance")
            String path
    ) {

    }
}
