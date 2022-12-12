/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

public class IdentityHubApiConfiguration extends WebServiceConfiguration {
    public IdentityHubApiConfiguration(WebServiceConfiguration webServiceConfig) {
        contextAlias = webServiceConfig.getContextAlias();
        path = webServiceConfig.getPath();
        port = webServiceConfig.getPort();
    }
}
