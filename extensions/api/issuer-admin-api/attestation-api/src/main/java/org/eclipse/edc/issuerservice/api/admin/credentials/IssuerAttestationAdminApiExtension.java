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

import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.IssuerAttestationAdminApiController;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.issuerservice.api.admin.credentials.IssuerAttestationAdminApiExtension.NAME;

@Extension(value = NAME)
public class IssuerAttestationAdminApiExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credentials Admin API Extension";
    @Inject
    private WebService webService;
    @Inject
    private AttestationDefinitionService attestationDefinitionService;
    @Inject
    private AuthorizationService authorizationService;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(AttestationDefinition.class, this::findById);
        var controller = new IssuerAttestationAdminApiController(authorizationService, attestationDefinitionService);
        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, controller);
    }
    
    private AttestationDefinition findById(String id) {
        return attestationDefinitionService.getAttestationById(id).getContent();
    }
}
