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

package org.eclipse.edc.identityhub.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.jsonld.spi.JsonLd;
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
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.identityhub.api.CredentialsApiConfigurationExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;

@Extension(value = NAME)
public class CredentialsApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Storage API Extension";
    private static final String API_VERSION_JSON_FILE = "credentials-api-version.json";

    @Configuration
    private CredentialsApiConfiguration apiConfiguration;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        portMappingRegistry.register(new PortMapping(CREDENTIALS, apiConfiguration.port(), apiConfiguration.path()));
        jsonLd.registerContext(DSPACE_DCP_V_1_0_CONTEXT, DCP_SCOPE_V_1_0);
        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("credentials", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record CredentialsApiConfiguration(
            @Setting(key = "web.http." + CREDENTIALS + ".port", description = "Port for " + CREDENTIALS + " api context", defaultValue = 13131 + "")
            int port,
            @Setting(key = "web.http." + CREDENTIALS + ".path", description = "Path for " + CREDENTIALS + " api context", defaultValue = "/api/credentials")
            String path
    ) {

    }

}
