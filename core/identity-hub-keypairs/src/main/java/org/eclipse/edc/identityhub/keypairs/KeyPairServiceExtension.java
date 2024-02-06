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

package org.eclipse.edc.identityhub.keypairs;

import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.events.keypair.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

import static org.eclipse.edc.identityhub.keypairs.KeyPairServiceExtension.NAME;

@Extension(NAME)
public class KeyPairServiceExtension implements ServiceExtension {
    public static final String NAME = "KeyPair Service Extension";

    @Inject
    private Vault vault;
    @Inject
    private KeyPairResourceStore keyPairResourceStore;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private Clock clock;

    private KeyPairObservable observable;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public KeyPairService createParticipantService(ServiceExtensionContext context) {
        var service = new KeyPairServiceImpl(keyPairResourceStore, vault, context.getMonitor(), keyPairObservable());
        eventRouter.registerSync(ParticipantContextCreated.class, service);
        eventRouter.registerSync(ParticipantContextDeleted.class, service);
        eventRouter.registerSync(DidDocumentPublished.class, service);
        return service;
    }

    @Provider
    public KeyPairObservable keyPairObservable() {
        if (observable == null) {
            observable = new KeyPairObservableImpl();
            observable.registerListener(new KeyPairEventListenerImpl(clock, eventRouter));
        }
        return observable;
    }
}
