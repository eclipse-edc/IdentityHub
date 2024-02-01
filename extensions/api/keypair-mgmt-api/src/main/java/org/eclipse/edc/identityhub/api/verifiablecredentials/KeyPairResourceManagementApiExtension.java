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

import org.eclipse.edc.identityhub.api.configuration.ManagementApiConfiguration;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.KeyPairResourceApiController;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.verifiablecredentials.KeyPairResourceManagementApiExtension.NAME;

@Extension(NAME)
public class KeyPairResourceManagementApiExtension implements ServiceExtension {
    public static final String NAME = "KeyPairResource Management API Extension";

    @Inject
    private ManagementApiConfiguration apiConfiguration;
    @Inject
    private WebService webService;
    @Inject
    private KeyPairService keyPairService;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(KeyPairResource.class, this::findById);
        var controller = new KeyPairResourceApiController(authorizationService, keyPairService);
        webService.registerResource(apiConfiguration.getContextAlias(), controller);
    }

    private ParticipantResource findById(String keyPairId) {
        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", keyPairId))
                .build();
        return keyPairService.query(q)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
