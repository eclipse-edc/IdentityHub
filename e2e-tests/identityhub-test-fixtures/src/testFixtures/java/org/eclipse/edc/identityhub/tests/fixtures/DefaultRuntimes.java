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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
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
        String[] MODULES_OAUTH2 = new String[]{":dist:bom:issuerservice-oauth2-bom"};
        String[] SQL_OAUTH2_MODULES = new String[]{":dist:bom:issuerservice-oauth2-bom", ":dist:bom:issuerservice-feature-sql-bom"};

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint(ISSUERADMIN, () -> URI.create("http://localhost:" + getFreePort() + "/api/admin"))
                .endpoint(ISSUANCE_API, () -> URI.create("http://localhost:" + getFreePort() + "/api/issuance"))
                .endpoint(STS, () -> URI.create("http://localhost:" + getFreePort() + "/api/sts"))
                .endpoint(IDENTITY, () -> URI.create("http://localhost:" + getFreePort() + "/api/identity"))
                .endpoint(IH_DID, () -> URI.create("http://localhost:" + getFreePort() + "/"))
                .endpoint("statuslist", () -> URI.create("http://localhost:" + getFreePort() + "/statuslist"));

        String SIGNING_KEY_ALIAS = "signing-key";

        static Config config() {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                    put("edc.issuer.statuslist.signing.key.alias", SIGNING_KEY_ALIAS);
                    put("edc.iam.did.web.use.https", "false");
                    put("edc.encryption.strict", "false");
                }
            });
        }

        static ServiceExtension seedSigningKeyFor(String participantContextId) {
            return new ServiceExtension() {
                @Inject
                private Vault vault;

                @Override
                public String name() {
                    return "Seed signing key for participantContextId " + participantContextId;
                }

                @Override
                public void initialize(ServiceExtensionContext context) {
                    try {
                        vault.storeSecret(participantContextId, SIGNING_KEY_ALIAS, new ECKeyGenerator(Curve.P_256).generate().toJSONString());
                    } catch (JOSEException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    interface IdentityHub {
        String[] MODULES = new String[]{":dist:bom:identityhub-bom"};
        String[] MODULES_OAUTH2 = new String[]{":dist:bom:identityhub-oauth2-bom"};
        String[] SQL_MODULES = new String[]{":dist:bom:identityhub-bom", ":dist:bom:identityhub-feature-sql-bom"};
        String[] SQL_OAUTH2_MODULES = new String[]{":dist:bom:identityhub-oauth2-bom", ":dist:bom:identityhub-feature-sql-bom"};

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
                    put("edc.encryption.strict", "false");
                    put("edc.iam.credential.revocation.mimetype", "*/*");
                }
            });
        }
    }
}
