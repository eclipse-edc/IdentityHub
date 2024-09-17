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

package org.eclipse.edc.identityhub.participantcontext;

import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.participantcontext.ParticipantContextExtension.NAME;

@Extension(NAME)
public class ParticipantContextExtension implements ServiceExtension {
    public static final String NAME = "ParticipantContext Extension";

    @Inject
    private ParticipantContextStore participantContextStore;
    @Inject
    private Vault vault;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private KeyPairService keyPairService;
    @Inject
    private Clock clock;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private DidResourceStore didResourceStore;

    @Inject(required = false)
    private AccountProvisioner accountProvisioner;

    private ParticipantContextObservable participantContextObservable;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ParticipantContextService createParticipantService() {
        return new ParticipantContextServiceImpl(participantContextStore, didResourceStore, vault, transactionContext, participantContextObservable(), accountProvisioner());
    }

    @Provider
    public ParticipantContextObservable participantContextObservable() {
        if (participantContextObservable == null) {
            participantContextObservable = new ParticipantContextObservableImpl();
            participantContextObservable.registerListener(new ParticipantContextEventPublisher(clock, eventRouter));
        }
        return participantContextObservable;
    }

    private AccountProvisioner accountProvisioner() {
        return ofNullable(accountProvisioner)
                .orElseGet(() -> manifest -> ServiceResult.success()); // default is a NOOP
    }
}
