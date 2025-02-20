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
import jakarta.json.Json;
import org.eclipse.edc.iam.identitytrust.transform.to.JwtToVerifiableCredentialTransformer;
import org.eclipse.edc.identityhub.api.storage.StorageApiController;
import org.eclipse.edc.identityhub.api.validation.CredentialMessageValidator;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialRequestMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialMessageTransformer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.identityhub.api.StorageApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.STORAGE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class StorageApiExtension implements ServiceExtension {

    public static final String NAME = "Storage API Extension";
    private static final String API_VERSION_JSON_FILE = "storage-api-version.json";

    @Configuration
    private StorageApiConfiguration apiConfiguration;
    @Inject
    private TypeTransformerRegistry typeTransformer;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private WebService webService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private CredentialWriter writer;
    @Inject
    private DcpIssuerTokenVerifier issuerTokenVerifier;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextString = STORAGE;

        portMappingRegistry.register(new PortMapping(contextString, apiConfiguration.port(), apiConfiguration.path()));

        validatorRegistry.register(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_MESSAGE_TERM), new CredentialMessageValidator());


        var controller = new StorageApiController(validatorRegistry, typeTransformer, jsonLd, writer, context.getMonitor(), issuerTokenVerifier);
        webService.registerResource(contextString, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(contextString, controller);

        jsonLd.registerContext(DSPACE_DCP_V_1_0_CONTEXT, DCP_SCOPE_V_1_0);

        registerTransformers(DCP_SCOPE_V_1_0, DSPACE_DCP_NAMESPACE_V_1_0);

        registerVersionInfo(getClass().getClassLoader());
    }

    void registerTransformers(String scope, JsonLdNamespace namespace) {

        var factory = Json.createBuilderFactory(Map.of());
        var scopedTransformerRegistry = typeTransformer.forContext(scope);
        scopedTransformerRegistry.register(new JsonObjectToCredentialMessageTransformer(typeManager, JSON_LD, namespace));
        scopedTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));
        scopedTransformerRegistry.register(new JsonObjectFromCredentialMessageTransformer(factory, typeManager, JSON_LD, namespace));
        scopedTransformerRegistry.register(new JsonObjectFromCredentialRequestMessageTransformer(factory, typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));

        typeTransformer.register(new JwtToVerifiableCredentialTransformer(monitor));

        // no need to register a JsonObject -> VerifiableCredential transformer here, because LD-Credentials
        // in the CredentialContainer would be JSON-literals, so they can be converted using an ObjectMapper
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("storage", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record StorageApiConfiguration(
            @Setting(key = "web.http." + STORAGE + ".port", description = "Port for " + STORAGE + " api context", defaultValue = 14141 + "")
            int port,
            @Setting(key = "web.http." + STORAGE + ".path", description = "Path for " + STORAGE + " api context", defaultValue = "/api/storage")
            String path
    ) {

    }

}
