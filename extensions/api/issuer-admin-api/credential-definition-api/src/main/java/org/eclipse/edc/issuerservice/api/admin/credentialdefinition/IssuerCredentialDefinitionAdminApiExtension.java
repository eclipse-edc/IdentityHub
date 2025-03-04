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

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition;

import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.IssuerCredentialDefinitionAdminApiController;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.issuerservice.api.admin.credentialdefinition.IssuerCredentialDefinitionAdminApiExtension.NAME;

@Extension(value = NAME)
public class IssuerCredentialDefinitionAdminApiExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credential Definition Admin API Extension";
    @Inject
    private WebService webService;
    @Inject
    private CredentialDefinitionService credentialDefinitionService;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(CredentialDefinition.class, this::findById);
        var controller = new IssuerCredentialDefinitionAdminApiController(authorizationService, credentialDefinitionService);
        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, controller);
    }

    public CredentialDefinition findById(String id) {
        return credentialDefinitionService.findCredentialDefinitionById(id).getContent();
    }
}
