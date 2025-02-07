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

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.StatusListInfoFactoryRegistryImpl;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListFactory;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialService;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
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

import static org.eclipse.edc.issuerservice.credentials.CredentialServiceExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class CredentialServiceExtension implements ServiceExtension {
    public static final String NAME = "Issuer Service Credential Service Extension";
    public static final String BITSTRING_STATUS_LIST_ENTRY = "BitstringStatusListEntry";

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
    public CredentialService getStatusListService(ServiceExtensionContext context) {
        var fact = getFactory();

        // Bitstring StatusList is provided by default. others can be added via extensions
        fact.register(BITSTRING_STATUS_LIST_ENTRY, new BitstringStatusListFactory(store));

        var tokenGenerationService = new JwtGenerationService(jwsSignerProvider);
        return new CredentialServiceImpl(store, transactionContext, typeManager.getMapper(JSON_LD), context.getMonitor(), tokenGenerationService,
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
