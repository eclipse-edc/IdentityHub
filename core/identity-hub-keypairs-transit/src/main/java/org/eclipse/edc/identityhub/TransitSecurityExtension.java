/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identityhub.transit.TransitEngine;
import org.eclipse.edc.identityhub.transit.TransitEngineImpl;
import org.eclipse.edc.jwt.spi.signer.JwsSignerProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory;

@Extension(value = TransitSecurityExtension.NAME)
public class TransitSecurityExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Transit Security Extension";

    @Inject
    private HashicorpVaultTokenProviderFactory tokenProviderFactory;
    @Inject
    private EdcHttpClient edcHttpClient;

    @Inject
    private TypeManager typeManager;

    @Setting(description = "The URL of the Hashicorp Vault", key = "edc.vault.hashicorp.url")
    private String vaultUrl;

    private TransitEngine transitEngine;

    @Provider
    public JwsSignerProvider jwsSignerProvider() {
        return new TransitJwsSignerProvider(transitEngine());
    }

    @Provider
    public TransitEngine transitEngine() {
        if (transitEngine == null) {
            // the engine mints a participant-scoped vault token per key (resource derived from the key name),
            // so it needs the factory rather than a single provider.
            transitEngine = new TransitEngineImpl(tokenProviderFactory, typeManager.getMapper(), edcHttpClient, vaultUrl);
        }

        return transitEngine;
    }
}
