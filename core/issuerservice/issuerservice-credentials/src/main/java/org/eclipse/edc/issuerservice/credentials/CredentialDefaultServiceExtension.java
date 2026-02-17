/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.StatusListInfoFactoryRegistryImpl;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(CredentialDefaultServiceExtension.NAME)
public class CredentialDefaultServiceExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credential Default Services";

    @Inject
    private StatusListCredentialPublisher credentialPublisher;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStore store;
    @Inject
    private CredentialGeneratorRegistry registry;
    @Inject
    private IdentityHubParticipantContextService participantContextService;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StatusListInfoFactoryRegistry statusListInfoFactoryRegistry() {
        return new StatusListInfoFactoryRegistryImpl();
    }

    @Provider(isDefault = true)
    public StatusListManager statusListManager() {
        return new BitstringStatusListManager(store, transactionContext, registry, participantContextService, credentialPublisher);
    }
}
