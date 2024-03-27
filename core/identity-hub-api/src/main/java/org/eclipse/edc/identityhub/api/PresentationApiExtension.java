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

import org.eclipse.edc.core.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.identityhub.api.v1.PresentationApiController;
import org.eclipse.edc.identityhub.api.validation.PresentationQueryValidator;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage;
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

import static org.eclipse.edc.identityhub.api.PresentationApiExtension.NAME;
import static org.eclipse.edc.identitytrust.VcConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class PresentationApiExtension implements ServiceExtension {

    public static final String NAME = "Presentation API Extension";
    public static final String RESOLUTION_SCOPE = "resolution-scope";
    public static final String RESOLUTION_CONTEXT = "resolution";
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
    private VerifiablePresentationService verifiablePresentationService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ParticipantContextService participantContextService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // setup validator
        validatorRegistry.register(PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY, new PresentationQueryValidator());


        var controller = new PresentationApiController(validatorRegistry, typeTransformer, credentialResolver, accessTokenVerifier, verifiablePresentationService, context.getMonitor(), participantContextService);

        var jsonLdMapper = typeManager.getMapper(JSON_LD);
        webService.registerResource(RESOLUTION_CONTEXT, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(RESOLUTION_CONTEXT, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, RESOLUTION_SCOPE));
        webService.registerResource(RESOLUTION_CONTEXT, controller);

        jsonLd.registerContext(IATP_CONTEXT_URL, RESOLUTION_SCOPE);

        // register transformer -- remove once registration is handled in EDC
        typeTransformer.register(new JsonObjectToPresentationQueryTransformer(jsonLdMapper));
        typeTransformer.register(new JsonValueToGenericTypeTransformer(jsonLdMapper));
    }


}
