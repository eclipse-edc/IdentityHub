/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.participant;

import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

/**
 * Deletes the credentials, credential requests and credential offers a participant context holds, once the participant
 * context is deleted, so that a participant context that is created later with the same ID does not inherit them.
 * Credentials that an issuer keeps, e.g. status list credentials, are not deleted.
 */
public class HolderDataCleanup implements EventSubscriber {
    private final CredentialStore credentialStore;
    private final HolderCredentialRequestStore credentialRequestStore;
    private final CredentialOfferStore credentialOfferStore;
    private final TransactionContext transactionContext;
    private final Monitor monitor;

    public HolderDataCleanup(CredentialStore credentialStore, HolderCredentialRequestStore credentialRequestStore,
                             CredentialOfferStore credentialOfferStore, TransactionContext transactionContext, Monitor monitor) {
        this.credentialStore = credentialStore;
        this.credentialRequestStore = credentialRequestStore;
        this.credentialOfferStore = credentialOfferStore;
        this.transactionContext = transactionContext;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof ParticipantContextDeleted deleted) {
            deleteHolderData(deleted.getParticipantContextId());
        } else {
            monitor.warning("Received event with unexpected payload type: %s".formatted(event.getPayload().getClass()));
        }
    }

    private void deleteHolderData(String participantContextId) {
        transactionContext.execute(() -> {
            var credentialQuery = queryByParticipantContextId(participantContextId)
                    .filter(new Criterion("usage", "=", CredentialUsage.Holder.toString()))
                    .build();
            credentialStore.query(credentialQuery)
                    .onSuccess(credentials -> deleteAll(participantContextId, "credentials", credentials,
                            credential -> credentialStore.deleteById(participantContextId, credential.getId())))
                    .onFailure(f -> warn(participantContextId, "credentials", f.getFailureDetail()));

            var query = queryByParticipantContextId(participantContextId).build();
            deleteAll(participantContextId, "credential requests", credentialRequestStore.query(query),
                    request -> credentialRequestStore.deleteById(request.getId()));
            deleteAll(participantContextId, "credential offers", credentialOfferStore.query(query),
                    offer -> credentialOfferStore.deleteById(offer.getId()));
        });
    }

    private <T> void deleteAll(String participantContextId, String type, Collection<T> items, Function<T, StoreResult<Void>> delete) {
        var errors = items.stream()
                .map(delete)
                .filter(StoreResult::failed)
                .map(AbstractResult::getFailureDetail)
                .collect(Collectors.joining(", "));
        if (!errors.isEmpty()) {
            warn(participantContextId, type, errors);
        }
    }

    private void warn(String participantContextId, String type, String detail) {
        monitor.warning("Removing the %s of deleted participant context '%s' failed, they must be deleted manually: %s"
                .formatted(type, participantContextId, detail));
    }
}
