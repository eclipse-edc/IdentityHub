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

package org.eclipse.edc.identityhub.api.verifiablecredential;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.identityhub.api.verifiablecredential.v1.unstable.ParticipantContextApiController;
import org.eclipse.edc.identityhub.api.verifiablecredential.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.verifiablecredential.ParticipantContextManagementApiExtension.NAME;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;

@Extension(value = NAME)
public class ParticipantContextManagementApiExtension implements ServiceExtension {

    public static final String NAME = "ParticipantContext management Identity API Extension";
    @Inject
    private WebService webService;
    @Inject
    private IdentityHubParticipantContextService participantContextService;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(IdentityHubParticipantContext.class, this::findByOwnerAndId);
        var controller = new ParticipantContextApiController(new ParticipantManifestValidator(monitor), participantContextService, authorizationService);
        webService.registerResource(IdentityHubApiContext.IDENTITY, controller);
    }

    private ParticipantResource findByOwnerAndId(String owner, String participantContextId) {
        return participantContextService.getParticipantContext(participantContextId).orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));
    }
}
