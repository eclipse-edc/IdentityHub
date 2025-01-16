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

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.api.authorization.AuthorizationServiceImpl;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.identityhub.api.ApiAuthorizationExtension.NAME;

@Extension(NAME)
public class ApiAuthorizationExtension implements ServiceExtension {

    public static final String NAME = "Identity API Authorization Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public AuthorizationService createAuthService() {
        return new AuthorizationServiceImpl();
    }
}
