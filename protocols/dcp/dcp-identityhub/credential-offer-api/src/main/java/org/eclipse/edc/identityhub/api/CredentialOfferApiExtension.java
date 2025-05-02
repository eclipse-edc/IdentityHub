/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.api.credentialoffer.CredentialOfferApiController;
import org.eclipse.edc.identityhub.api.validation.CredentialOfferMessageValidator;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialObjectTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialOfferMessageTransformer;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.api.CredentialOfferApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class CredentialOfferApiExtension implements ServiceExtension {

    public static final String NAME = "Storage API Extension";

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
    private CredentialWriter writer;
    @Inject
    private DcpIssuerTokenVerifier issuerTokenVerifier;
    @Inject
    private Monitor monitor;

    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private CredentialOfferService credentialOfferService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        validatorRegistry.register(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OFFER_MESSAGE_TERM), new CredentialOfferMessageValidator());

        var controller = new CredentialOfferApiController(validatorRegistry, typeTransformer, issuerTokenVerifier, participantContextService, credentialOfferService, jsonLd);
        webService.registerResource(CREDENTIALS, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(CREDENTIALS, controller);

        registerTransformers();
    }

    void registerTransformers() {
        var scopedTransformerRegistry = typeTransformer.forContext(DCP_SCOPE_V_1_0);

        // inbound
        scopedTransformerRegistry.register(new JsonObjectToCredentialOfferMessageTransformer(DSPACE_DCP_NAMESPACE_V_1_0));
        scopedTransformerRegistry.register(new JsonObjectToCredentialObjectTransformer(typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));
    }
}
