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

import jakarta.json.Json;
import org.eclipse.edc.iam.identitytrust.transform.to.JwtToVerifiableCredentialTransformer;
import org.eclipse.edc.identityhub.api.storage.StorageApiController;
import org.eclipse.edc.identityhub.api.validation.CredentialMessageValidator;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialRequestMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialMessageTransformer;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.api.StorageApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class StorageApiExtension implements ServiceExtension {

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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextString = CREDENTIALS;

        validatorRegistry.register(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_MESSAGE_TERM), new CredentialMessageValidator());

        var controller = new StorageApiController(validatorRegistry, typeTransformer, jsonLd, writer, context.getMonitor().withPrefix("StorageAPI"), issuerTokenVerifier, participantContextService);
        webService.registerResource(contextString, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(contextString, controller);

        registerTransformers();
    }

    void registerTransformers() {

        var factory = Json.createBuilderFactory(Map.of());
        var scopedTransformerRegistry = typeTransformer.forContext(DCP_SCOPE_V_1_0);

        // inbound
        scopedTransformerRegistry.register(new JsonObjectToCredentialMessageTransformer(typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));
        scopedTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));

        //outbound
        scopedTransformerRegistry.register(new JsonObjectFromCredentialMessageTransformer(factory, typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));
        scopedTransformerRegistry.register(new JsonObjectFromCredentialRequestMessageTransformer(factory, typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));

        typeTransformer.register(new JwtToVerifiableCredentialTransformer(monitor));

        // no need to register a JsonObject -> VerifiableCredential transformer here, because LD-Credentials
        // in the CredentialContainer would be JSON-literals, so they can be converted using an ObjectMapper
    }
}
