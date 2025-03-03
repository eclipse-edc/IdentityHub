/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.verifiablecredentials;

import org.eclipse.edc.identityhub.api.verifiablecredential.validation.VerifiableCredentialManifestValidator;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.GetAllCredentialsApiController;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.VerifiableCredentialsApiController;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.transformer.VerifiableCredentialManifestToVerifiableCredentialResourceTransformer;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.AbstractParticipantResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.verifiablecredentials.VerifiableCredentialApiExtension.NAME;

@Extension(NAME)
public class VerifiableCredentialApiExtension implements ServiceExtension {
    public static final String NAME = "VerifiableCredentials API Extension";

    @Inject
    TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private WebService webService;
    @Inject
    private CredentialStore credentialStore;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private CredentialRequestManager credentialRequestManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(VerifiableCredentialResource.class, this::queryById);
        var registry = typeTransformerRegistry.forContext("identity-api");
        registry.register(new VerifiableCredentialManifestToVerifiableCredentialResourceTransformer());
        var controller = new VerifiableCredentialsApiController(credentialStore, authorizationService, new VerifiableCredentialManifestValidator(), registry, credentialRequestManager);
        var getAllController = new GetAllCredentialsApiController(credentialStore);
        webService.registerResource(IdentityHubApiContext.IDENTITY, controller);
        webService.registerResource(IdentityHubApiContext.IDENTITY, getAllController);
    }

    private AbstractParticipantResource queryById(String credentialId) {
        return credentialStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .map(list -> list.iterator().next())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
