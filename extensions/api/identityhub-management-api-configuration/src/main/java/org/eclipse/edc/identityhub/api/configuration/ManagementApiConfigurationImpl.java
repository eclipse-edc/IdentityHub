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

package org.eclipse.edc.identityhub.api.configuration;

import org.eclipse.edc.identityhub.spi.ManagementApiConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

/**
 * Marker class specifically made for the configuration of all our Management APIs
 */
class ManagementApiConfigurationImpl extends ManagementApiConfiguration {

    ManagementApiConfigurationImpl(String contextAlias) {
        super();
        this.contextAlias = contextAlias;
    }

    ManagementApiConfigurationImpl(WebServiceConfiguration webServiceConfiguration) {
        this.contextAlias = webServiceConfiguration.getContextAlias();
        this.path = webServiceConfiguration.getPath();
        this.port = webServiceConfiguration.getPort();
    }
}
