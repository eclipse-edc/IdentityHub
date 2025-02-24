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

package org.eclipse.edc.identityhub.tests.fixtures.issuerservice;

import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * The IssuerServiceRuntimeConfiguration class represents an IssuerService Runtime configuration and provides various information, such as API endpoints
 */
public class IssuerServiceRuntimeConfiguration extends AbstractRuntimeConfiguration {

    private Endpoint adminEndpoint;
    private Endpoint issuerApiEndpoint;

    public Endpoint getAdminEndpoint() {
        return adminEndpoint;
    }

    public Endpoint getIssuerApiEndpoint() {
        return issuerApiEndpoint;
    }


    public Config config() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.issueradmin.port", String.valueOf(adminEndpoint.getUrl().getPort()));
                put("web.http.issueradmin.path", adminEndpoint.getUrl().getPath());

                put("web.http.issuance.port", String.valueOf(issuerApiEndpoint.getUrl().getPort()));
                put("web.http.issuance.path", issuerApiEndpoint.getUrl().getPath());
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/.well-known/api");
                put("web.http.did.port", String.valueOf(didEndpoint.getUrl().getPort()));
                put("web.http.did.path", didEndpoint.getUrl().getPath());
                put("edc.sql.schema.autocreate", "true");
                put("edc.sts.account.api.url", "http://sts.com/accounts");
                put("edc.sts.accounts.api.auth.header.value", "password");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(false));
                put("edc.issuer.statuslist.signing.key.alias", "signing-key");
                // config for the embedded STS
                put("edc.iam.sts.publickey.id", "test-public-key");
                put("edc.iam.sts.privatekey.alias", "issuer-alias");
            }
        });
    }


    public static final class Builder extends AbstractRuntimeConfiguration.Builder<IssuerServiceRuntimeConfiguration, Builder> {

        private Builder() {
            super(new IssuerServiceRuntimeConfiguration());
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public IssuerServiceRuntimeConfiguration build() {
            super.build();
            participant.adminEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/admin"), Map.of());
            participant.issuerApiEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/issuance"), Map.of());
            return participant;
        }
    }

}
