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

import org.eclipse.edc.identityhub.api.participantcontext.v1.ParticipantContextApiController;
import org.eclipse.edc.identityhub.api.participantcontext.v1.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

import static org.eclipse.edc.identityhub.api.participantcontext.ParticipantContextManagementApiExtension.NAME;

@Extension(value = NAME)
public class ParticipantContextManagementApiExtension implements ServiceExtension {

    public static final String NAME = "ParticipantContext Management API Extension";
    @Inject
    private WebService webService;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private WebServiceConfiguration webServiceConfiguration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new ParticipantContextApiController(new ParticipantManifestValidator(), participantContextService);
//        webService.registerResource(webServiceConfiguration.getContextAlias(), controller);
    }
}
