/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.identitytrust.transform.from.JsonObjectFromPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.identityhub.api.v1.PresentationApiController;
import org.eclipse.edc.identityhub.api.validation.PresentationQueryValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.api.PresentationApiExtension.NAME;
import static org.eclipse.edc.identityhub.spi.IdentityHubApiContext.PRESENTATION;
import static org.eclipse.edc.identityhub.spi.IdentityHubApiContext.RESOLUTION;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class PresentationApiExtension implements ServiceExtension {

    public static final String NAME = "Presentation API Extension";
    public static final String PRESENTATION_SCOPE = "presentation-scope";
    private static final String API_VERSION_JSON_FILE = "presentation-api-version.json";

    @Configuration
    private PresentationApiConfiguration apiConfiguration;

    @Inject
    private TypeTransformerRegistry typeTransformer;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private WebService webService;
    @Inject
    private SelfIssuedTokenVerifier selfIssuedTokenVerifier;
    @Inject
    private CredentialQueryResolver credentialResolver;
    @Inject
    private VerifiablePresentationService verifiablePresentationService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ParticipantContextService participantContextService;
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
        var contextString = determineApiContext(context);

        portMappingRegistry.register(new PortMapping(contextString, apiConfiguration.port(), apiConfiguration.path()));
        validatorRegistry.register(PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY, new PresentationQueryValidator());

        var jsonLdMapper = typeManager.getMapper(JSON_LD);


        var controller = new PresentationApiController(validatorRegistry, typeTransformer, credentialResolver, selfIssuedTokenVerifier, verifiablePresentationService, context.getMonitor(), participantContextService);
        webService.registerResource(contextString, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(contextString, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, PRESENTATION_SCOPE));
        webService.registerResource(contextString, controller);

        jsonLd.registerContext(DCP_CONTEXT_URL, PRESENTATION_SCOPE);

        // register transformer -- remove once registration is handled in EDC
        typeTransformer.register(new JsonObjectToPresentationQueryTransformer(jsonLdMapper));
        typeTransformer.register(new JsonValueToGenericTypeTransformer(jsonLdMapper));
        typeTransformer.register(new JsonObjectFromPresentationResponseMessageTransformer());

        registerVersionInfo(getClass().getClassLoader());
    }

    private String determineApiContext(ServiceExtensionContext context) {

        if (context.getConfig("web.http").getRelativeEntries(PRESENTATION).isEmpty() && !context.getConfig("web.http").getRelativeEntries(RESOLUTION).isEmpty()) {
            context.getMonitor().warning("Deprecated config: 'web.http.%s.* was replaced by 'web.http.%s.*', please update at your earliest convenience.".formatted(RESOLUTION, PRESENTATION));
            return RESOLUTION;
        }
        return PRESENTATION;
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("presentation", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record PresentationApiConfiguration(
            @Setting(key = "web.http." + PRESENTATION + ".port", description = "Port for " + PRESENTATION + " api context", defaultValue = 13131 + "")
            int port,
            @Setting(key = "web.http." + PRESENTATION + ".path", description = "Path for " + PRESENTATION + " api context", defaultValue = "/api/presentation")
            String path
    ) {

    }

}
