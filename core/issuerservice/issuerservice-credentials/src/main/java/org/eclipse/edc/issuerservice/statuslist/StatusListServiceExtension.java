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

package org.eclipse.edc.issuerservice.statuslist;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListService;
import org.eclipse.edc.issuerservice.statuslist.bitstring.BitstringStatusListFactory;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.issuerservice.statuslist.StatusListServiceExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class StatusListServiceExtension implements ServiceExtension {
    public static final String NAME = "Status List Service Extension";

    @Setting(description = "Alias for the private key that is intended for signing status list credentials", key = "edc.issuer.statuslist.signing.key.alias")
    private String privateKeyAlias;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStore store;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JwsSignerProvider jwsSignerProvider;
    private StatusListInfoFactoryRegistry factory;

    @Provider
    public StatusListService getStatusListService(ServiceExtensionContext context) {
        var fact = getFactory();

        // Bitstring StatusList is provided by default. others can be added via extensions
        fact.register("BitstringStatusListEntry", new BitstringStatusListFactory(store, typeManager.getMapper()));

        var tokenGenerationService = new JwtGenerationService(jwsSignerProvider);
        return new StatusListServiceImpl(store, transactionContext, typeManager.getMapper(JSON_LD), context.getMonitor(), tokenGenerationService,
                () -> privateKeyAlias, fact);
    }

    @Provider
    public StatusListInfoFactoryRegistry getFactory() {
        if (factory == null) {
            factory = new StatusListInfoFactoryRegistryImpl();
        }
        return factory;
    }
}
