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

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.api.credentialoffer.CredentialObjectResolver;
import org.eclipse.edc.identityhub.api.credentialoffer.CredentialOfferApiController;
import org.eclipse.edc.identityhub.api.validation.CredentialOfferMessageValidator;
import org.eclipse.edc.identityhub.http.IssuerEndpoints;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialObjectTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToCredentialOfferMessageTransformer;
import org.eclipse.edc.identityhub.protocols.dcp.transform.to.JsonObjectToIssuerMetadataTransformer;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JsonMapperProvider;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.api.CredentialOfferApiExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class CredentialOfferApiExtension implements ServiceExtension {

    public static final String NAME = "Storage API Extension";

    @Setting(description = IssuerEndpoints.RESTRICT_DESCRIPTION, key = IssuerEndpoints.RESTRICT_SETTING, defaultValue = "false")
    private boolean restrictIssuerEndpoints;

    @Setting(description = IssuerEndpoints.ALLOWED_HOSTS_DESCRIPTION, key = IssuerEndpoints.ALLOWED_HOSTS_SETTING, required = false)
    private String allowedIssuerHosts;

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
    private DcpIssuerTokenVerifier issuerTokenVerifier;

    @Inject
    private IdentityHubParticipantContextService participantContextService;
    @Inject
    private CredentialOfferService credentialOfferService;
    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private RetryPolicy<Response> retryPolicy;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        validatorRegistry.register(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OFFER_MESSAGE_TERM), new CredentialOfferMessageValidator());

        // the Issuer's endpoint is taken from the DID document of whoever sent the offer
        var issuerHttpClient = restrictIssuerEndpoints
                ? new EdcHttpClientImpl(IssuerEndpoints.restrict(okHttpClient, IssuerEndpoints.parseAllowedHosts(allowedIssuerHosts)), retryPolicy, context.getMonitor())
                : httpClient;
        var credentialObjectResolver = new CredentialObjectResolver(didResolverRegistry, issuerHttpClient, jsonLd, typeTransformer, context.getMonitor().withPrefix("CredentialOfferAPI"));
        var controller = new CredentialOfferApiController(validatorRegistry, typeTransformer, issuerTokenVerifier, participantContextService, credentialOfferService, jsonLd, credentialObjectResolver,
                context.getMonitor().withPrefix("CredentialOfferAPI"));
        webService.registerResource(CREDENTIALS, new JsonMapperProvider(typeManager, JSON_LD));
        webService.registerResource(CREDENTIALS, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(CREDENTIALS, controller);

        registerTransformers();
    }

    void registerTransformers() {
        var scopedTransformerRegistry = typeTransformer.forContext(DCP_SCOPE_V_1_0);

        // inbound
        scopedTransformerRegistry.register(new JsonObjectToCredentialOfferMessageTransformer(DSPACE_DCP_NAMESPACE_V_1_0));
        scopedTransformerRegistry.register(new JsonObjectToCredentialObjectTransformer(typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));
        scopedTransformerRegistry.register(new JsonObjectToIssuerMetadataTransformer(DSPACE_DCP_NAMESPACE_V_1_0));
    }
}
