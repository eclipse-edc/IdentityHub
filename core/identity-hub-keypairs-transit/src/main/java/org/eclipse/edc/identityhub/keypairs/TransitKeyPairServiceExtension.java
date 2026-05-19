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

package org.eclipse.edc.identityhub.keypairs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.transit.TransitEngineImpl;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import static org.eclipse.edc.identityhub.keypairs.TransitKeyPairServiceExtension.NAME;

@Extension(NAME)
public class TransitKeyPairServiceExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Transit KeyPair Service Extension";
    @Setting(description = "The URL of the Hashicorp Vault", key = "edc.vault.hashicorp.url")
    private String vaultUrl;

    @Inject
    private KeyPairResourceStore keyPairResourceStore;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private ParticipantContextStore participantContextService;

    @Inject
    private KeyPairObservable keyPairObservable;
    @Inject
    private HashicorpVaultTokenProvider tokenProvider;
    @Inject
    private EdcHttpClient edcHttpClient;


    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public KeyPairService createKeyPairService(ServiceExtensionContext context) {
        var transitEngine = new TransitEngineImpl(tokenProvider, new ObjectMapper(), edcHttpClient, vaultUrl);
        var service = new TransitKeyPairService(keyPairResourceStore, context.getMonitor().withPrefix("KeyPairService"), keyPairObservable, transactionContext, participantContextService, transitEngine);
        eventRouter.registerSync(ParticipantContextDeleted.class, service);
        return service;
    }
}
