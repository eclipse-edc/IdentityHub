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

package org.eclipse.edc.issuerservice.api.admin.credentials;

import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants;
import org.eclipse.edc.identityhub.protocols.dcp.transform.from.JsonObjectFromCredentialOfferMessageTransformer;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.IssuerCredentialsAdminApiController;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.issuerservice.api.admin.credentials.IssuerCredentialsAdminApiExtension.NAME;

@Extension(value = NAME)
public class IssuerCredentialsAdminApiExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credentials Admin API Extension";
    @Inject
    private WebService webService;
    @Inject
    private CredentialStatusService credentialService;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private IssuerCredentialOfferService credentialOfferService;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        authorizationService.addLookupFunction(VerifiableCredentialResource.class, this::findById);
        var controller = new IssuerCredentialsAdminApiController(authorizationService, credentialService, credentialOfferService);
        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, controller);

        // required for sending CredentialOffer messages to the holder
        typeTransformerRegistry.forContext(DcpConstants.DCP_SCOPE_V_1_0).register(new JsonObjectFromCredentialOfferMessageTransformer(DSPACE_DCP_NAMESPACE_V_1_0));
    }


    public VerifiableCredentialResource findById(String id) {
        return credentialService.getCredentialById(id).getContent();
    }
}
