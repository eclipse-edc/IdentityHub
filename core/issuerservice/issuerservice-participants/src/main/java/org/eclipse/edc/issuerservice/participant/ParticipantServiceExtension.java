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

package org.eclipse.edc.issuerservice.participant;

import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.issuerservice.participant.ParticipantServiceExtension.NAME;

@Extension(value = NAME)
public class ParticipantServiceExtension implements ServiceExtension {
    public static final String NAME = "IssuerService Participant Service Extension";
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private ParticipantStore store;

    @Provider
    public ParticipantService getParticipantService() {
        return new ParticipantServiceImpl(transactionContext, store);
    }
}
