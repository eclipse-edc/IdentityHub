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
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;

import static org.eclipse.edc.spi.result.ServiceResult.from;

public class ParticipantServiceImpl implements ParticipantService {
    private final TransactionContext transactionContext;
    private final ParticipantStore participantStore;

    public ParticipantServiceImpl(TransactionContext transactionContext, ParticipantStore participantStore) {
        this.transactionContext = transactionContext;
        this.participantStore = participantStore;
    }

    @Override
    public ServiceResult<Void> createParticipant(Participant participant) {
        return transactionContext.execute(() -> from(participantStore.create(participant)));
    }

    @Override
    public ServiceResult<Void> deleteParticipant(String participantId) {
        return transactionContext.execute(() -> from(participantStore.deleteById(participantId)));
    }

    @Override
    public ServiceResult<Void> updateParticipant(Participant participant) {
        return transactionContext.execute(() -> from(participantStore.update(participant)));
    }

    @Override
    public ServiceResult<Collection<Participant>> queryParticipants(QuerySpec querySpec) {
        return transactionContext.execute(() -> from(participantStore.query(querySpec)));
    }

    @Override
    public ServiceResult<Participant> findById(String participantId) {
        return transactionContext.execute(() -> from(participantStore.findById(participantId)));
    }
}
