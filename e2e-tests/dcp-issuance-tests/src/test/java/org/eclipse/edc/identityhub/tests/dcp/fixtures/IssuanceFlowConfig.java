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

package org.eclipse.edc.identityhub.tests.dcp.fixtures;

import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceRuntimeConfiguration;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;

public class IssuanceFlowConfig {


    public static Config issuerConfig(IssuerServiceRuntimeConfiguration cfg) {
        return ConfigFactory.fromMap(Map.of(
                "edc.iam.did.web.use.https", "false"));
    }

    public static Config identityHubConfig(IdentityHubRuntimeConfiguration cfg) {
        var did = cfg.didFor("user1");
        return ConfigFactory.fromMap(Map.of(
                "edc.ih.iam.id", did,
                "edc.iam.did.web.use.https", "false"));
    }

}
