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

package org.eclipse.edc.identityhub.api.participantcontext;

import org.eclipse.edc.identityhub.api.participantcontext.v1.unstable.ParticipantContextApiController;
import org.eclipse.edc.identityhub.api.v1.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.IdentityHubApiContext;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.participantcontext.ParticipantContextManagementApiExtension.NAME;
import static org.eclipse.edc.identityhub.spi.AuthorizationResultHandler.exceptionMapper;

@Extension(value = NAME)
public class ParticipantContextManagementApiExtension implements ServiceExtension {

    public static final String NAME = "ParticipantContext Management API Extension";
    @Inject
    private WebService webService;
    @Inject
    private ParticipantContextService participantContextService;
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
        authorizationService.addLookupFunction(ParticipantContext.class, s -> participantContextService.getParticipantContext(s).orElseThrow(exceptionMapper(ParticipantContext.class, s)));
        var controller = new ParticipantContextApiController(new ParticipantManifestValidator(monitor), participantContextService, authorizationService);
        webService.registerResource(IdentityHubApiContext.IDENTITY, controller);
    }
}
