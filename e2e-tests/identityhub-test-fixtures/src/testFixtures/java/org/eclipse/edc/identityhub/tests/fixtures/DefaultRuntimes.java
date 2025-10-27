/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.HashMap;

import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.CREDENTIALS;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.IDENTITY;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.IH_DID;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUANCE_API;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.ISSUERADMIN;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.STS;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Defines default runtime configurations for Issuer and Identity Hub services used in tests.
 */
public interface DefaultRuntimes {

    interface Issuer {

        String[] MODULES = new String[]{":dist:bom:issuerservice-bom"};
        String[] SQL_MODULES = new String[]{":dist:bom:issuerservice-bom", ":dist:bom:issuerservice-feature-sql-bom"};

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint(ISSUERADMIN, () -> URI.create("http://localhost:" + getFreePort() + "/api/admin"))
                .endpoint(ISSUANCE_API, () -> URI.create("http://localhost:" + getFreePort() + "/api/issuance"))
                .endpoint(STS, () -> URI.create("http://localhost:" + getFreePort() + "/api/sts"))
                .endpoint(IDENTITY, () -> URI.create("http://localhost:" + getFreePort() + "/api/identity"))
                .endpoint(IH_DID, () -> URI.create("http://localhost:" + getFreePort() + "/"))
                .endpoint("statuslist", () -> URI.create("http://localhost:" + getFreePort() + "/statuslist"));


        static Config config() {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                    put("edc.issuer.statuslist.signing.key.alias", "signing-key");
                    put("edc.iam.did.web.use.https", "false");
                }
            });
        }
    }

    interface IdentityHub {
        String[] MODULES = new String[]{":dist:bom:identityhub-bom"};
        String[] SQL_MODULES = new String[]{":dist:bom:identityhub-bom", ":dist:bom:identityhub-feature-sql-bom"};

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint(CREDENTIALS, () -> URI.create("http://localhost:" + getFreePort() + "/api/credentials"))
                .endpoint(STS, () -> URI.create("http://localhost:" + getFreePort() + "/api/sts"))
                .endpoint(IDENTITY, () -> URI.create("http://localhost:" + getFreePort() + "/api/identity"))
                .endpoint(IH_DID, () -> URI.create("http://localhost:" + getFreePort() + "/"));

        static Config config() {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                    put("edc.iam.sts.publickey.id", "test-public-key");
                    put("edc.iam.sts.privatekey.alias", "user1-alias"); //this must be "username"-alias
                    put("edc.iam.did.web.use.https", "false");
                }
            });
        }
    }
}
