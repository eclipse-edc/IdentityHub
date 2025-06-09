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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.json.Json;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest.CredentialRequestApiController;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus.CredentialRequestStatusApiController;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.issuermetadata.IssuerMetadataApiController;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialObjectTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialOfferMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialRequestStatusTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromIssuerMetadataTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialRequestMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.validation.CredentialRequestMessageValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
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
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.IssuerApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUANCE_API;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class IssuerApiExtension implements ServiceExtension {
    public static final String NAME = "Issuer API extension";

    private static final String API_VERSION_JSON_FILE = "issuer-api-version.json";

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

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private DcpIssuerService dcpIssuerService;

    @Inject
    private DcpHolderTokenVerifier dcpHolderTokenVerifier;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private ParticipantContextService participantContextService;

    @Inject
    private IssuanceProcessService issuanceProcessService;

    @Inject
    private DcpIssuerMetadataService issuerMetadataService;

    @Override
    public void initialize(ServiceExtensionContext context) {

        portMappingRegistry.register(new PortMapping(ISSUANCE_API, apiConfiguration.port(), apiConfiguration.path()));

        var dcpRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);
        registerTransformers(dcpRegistry, DSPACE_DCP_NAMESPACE_V_1_0);
        registerValidators(DSPACE_DCP_NAMESPACE_V_1_0);

        webService.registerResource(ISSUANCE_API, new CredentialRequestApiController(participantContextService, dcpIssuerService, dcpHolderTokenVerifier, validatorRegistry, dcpRegistry, DSPACE_DCP_NAMESPACE_V_1_0));
        webService.registerResource(ISSUANCE_API, new CredentialRequestStatusApiController(participantContextService, dcpHolderTokenVerifier, issuanceProcessService, dcpRegistry));
        webService.registerResource(ISSUANCE_API, new IssuerMetadataApiController(participantContextService, issuerMetadataService, dcpRegistry));

        webService.registerResource(ISSUANCE_API, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(ISSUANCE_API, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, DCP_SCOPE_V_1_0));

        jsonLd.registerContext(DSPACE_DCP_V_1_0_CONTEXT, DCP_SCOPE_V_1_0);

        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerTransformers(TypeTransformerRegistry dcpRegistry, JsonLdNamespace namespace) {

        var builderFactory = Json.createBuilderFactory(Map.of());
        // from
        dcpRegistry.register(new JsonObjectFromCredentialRequestStatusTransformer(namespace, builderFactory));
        dcpRegistry.register(new JsonObjectFromIssuerMetadataTransformer(namespace));
        dcpRegistry.register(new JsonObjectFromCredentialObjectTransformer(typeManager, JSON_LD, namespace));
        dcpRegistry.register(new JsonObjectFromCredentialOfferMessageTransformer(namespace));

        // to
        dcpRegistry.register(new JsonObjectToCredentialRequestMessageTransformer(typeManager, JSON_LD, namespace));
    }

    private void registerValidators(JsonLdNamespace namespace) {

        validatorRegistry.register(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM), CredentialRequestMessageValidator.instance(namespace));
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file '%s' not found or not readable.".formatted(API_VERSION_JSON_FILE));
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(vr -> apiVersionService.addRecord("issuer-api", vr));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record CredentialRequestApiConfiguration(
            @Setting(key = "web.http." + ISSUANCE_API + ".port", description = "Port for " + ISSUANCE_API + " api context", defaultValue = 13132 + "")
            int port,
            @Setting(key = "web.http." + ISSUANCE_API + ".path", description = "Path for " + ISSUANCE_API + " api context", defaultValue = "/api/issuance")
            String path
    ) {

    }
}
