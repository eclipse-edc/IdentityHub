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

import org.eclipse.edc.iam.identitytrust.transform.from.JsonObjectFromPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.identityhub.api.validation.PresentationQueryValidator;
import org.eclipse.edc.identityhub.api.verifiablecredential.PresentationApiController;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.api.PresentationApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_0_8;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class PresentationApiExtension implements ServiceExtension {

    public static final String NAME = "Presentation API Extension";

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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextString = CREDENTIALS;

        registerValidator(DSPACE_DCP_NAMESPACE_V_0_8);
        registerValidator(DSPACE_DCP_NAMESPACE_V_1_0);


        var controller = new PresentationApiController(validatorRegistry, typeTransformer, credentialResolver, selfIssuedTokenVerifier,
                verifiablePresentationService, context.getMonitor().withPrefix("PresentationAPI"), participantContextService, jsonLd);
        webService.registerResource(contextString, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(contextString, controller);

        jsonLd.registerContext(DCP_CONTEXT_URL, DCP_SCOPE_V_0_8);

        registerTransformers(DCP_SCOPE_V_0_8, DSPACE_DCP_NAMESPACE_V_0_8);
        registerTransformers(DCP_SCOPE_V_1_0, DSPACE_DCP_NAMESPACE_V_1_0);
    }

    void registerTransformers(String scope, JsonLdNamespace namespace) {
        var scopedTransformerRegistry = typeTransformer.forContext(scope);
        // inbound
        scopedTransformerRegistry.register(new JsonObjectToPresentationQueryTransformer(typeManager, JSON_LD, namespace));
        scopedTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));

        // outbound
        scopedTransformerRegistry.register(new JsonObjectFromPresentationResponseMessageTransformer(namespace));
    }

    private void registerValidator(JsonLdNamespace namespace) {
        validatorRegistry.register(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM), new PresentationQueryValidator(namespace));
    }
}
