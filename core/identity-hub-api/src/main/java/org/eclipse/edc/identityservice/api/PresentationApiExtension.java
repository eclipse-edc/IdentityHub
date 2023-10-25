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

package org.eclipse.edc.identityservice.api;

import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identityhub.transform.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.identityservice.api.v1.PresentationApiController;
import org.eclipse.edc.identityservice.api.validation.PresentationQueryValidator;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

import java.net.URISyntaxException;

import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension(value = "Presentation API Extension")
public class PresentationApiExtension implements ServiceExtension {

    public static final String RESOLUTION_SCOPE = "resolution-scope";
    public static final String RESOLUTION_CONTEXT = "resolution";
    public static final String PRESENTATION_EXCHANGE_V_1_JSON = "presentation-exchange.v1.json";
    public static final String PRESENTATION_QUERY_V_08_JSON = "presentation-query.v08.json";
    @Inject
    private TypeTransformerRegistry typeTransformer;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private WebService webService;

    @Inject
    private AccessTokenVerifier accessTokenVerifier;

    @Inject
    private CredentialQueryResolver credentialResolver;

    @Inject
    private PresentationGenerator presentationGenerator;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // setup validator
        validatorRegistry.register(PresentationQuery.PRESENTATION_QUERY_TYPE_PROPERTY, new PresentationQueryValidator());


        // Setup API
        cacheContextDocuments(getClass().getClassLoader());
        var controller = new PresentationApiController(validatorRegistry, typeTransformer, credentialResolver, accessTokenVerifier, presentationGenerator, context.getMonitor());

        var jsonLdMapper = typeManager.getMapper(JSON_LD);
        webService.registerResource(RESOLUTION_CONTEXT, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(RESOLUTION_CONTEXT, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, RESOLUTION_SCOPE));
        webService.registerResource(RESOLUTION_CONTEXT, controller);

        // register transformer
        typeTransformer.register(new JsonObjectToPresentationQueryTransformer(jsonLdMapper));
        typeTransformer.register(new JsonValueToGenericTypeTransformer(jsonLdMapper));
    }

    private void cacheContextDocuments(ClassLoader classLoader) {
        try {
            jsonLd.registerCachedDocument(PRESENTATION_EXCHANGE_URL, classLoader.getResource(PRESENTATION_EXCHANGE_V_1_JSON).toURI());
            jsonLd.registerCachedDocument(IATP_CONTEXT_URL, classLoader.getResource(PRESENTATION_QUERY_V_08_JSON).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
