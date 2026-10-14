/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.credential.offer.handler;

import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferEvent;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Subscribes to {@link CredentialOfferEvent}s and implements a default processing behaviour.
 * For example, when receiving {@link CredentialOfferReceived} events, a credential issuance request is sent to the issuer,
 * unless a request to the same issuer for exactly the same credential types and formats is still open.
 */
public class CredentialOfferHandler implements EventSubscriber {
    private final Monitor monitor;
    private final CredentialRequestManager requestManager;
    private final DcpProfileRegistry dcpProfileRegistry;
    private final CredentialOfferStore credentialOfferStore;
    private final HolderCredentialRequestStore holderCredentialRequestStore;
    private final TransactionContext transactionContext;

    public CredentialOfferHandler(Monitor monitor, CredentialRequestManager requestManager, DcpProfileRegistry dcpProfileRegistry, CredentialOfferStore credentialOfferStore,
                                  HolderCredentialRequestStore holderCredentialRequestStore, TransactionContext transactionContext) {
        this.monitor = monitor;
        this.requestManager = requestManager;
        this.dcpProfileRegistry = dcpProfileRegistry;
        this.credentialOfferStore = credentialOfferStore;
        this.holderCredentialRequestStore = holderCredentialRequestStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof CredentialOfferReceived receivedEvent) {
            handleReceivedEvent(receivedEvent);
            return;
        }
        monitor.warning("Received wrong event type %s".formatted(event.getPayload().getClass()));
    }

    private void handleReceivedEvent(CredentialOfferReceived receivedEvent) {
        transactionContext.execute(() -> {
            var storedOffer = credentialOfferStore.findById(receivedEvent.getId());
            if (storedOffer == null) {
                monitor.warning("Could not load CredentialOffer from database, ID: %s".formatted(receivedEvent.getId()));
                return;
            }

            if (storedOffer.getStateAsEnum() == CredentialOfferStatus.PROCESSING || storedOffer.getStateAsEnum() == CredentialOfferStatus.PROCESSED) {
                monitor.warning("Credential offer with ID = '%s' is already processing. Skipping.".formatted(receivedEvent.getId()));
                return;
            }

            var typesAndFormats = extractTypes(storedOffer);

            if (typesAndFormats.isEmpty()) {
                monitor.warning("Could not process credential offer. See previous logs for more details.");
                storedOffer.transition(CredentialOfferStatus.REJECTED);
                save(storedOffer);
                return;
            }

            var openDuplicate = findOpenDuplicate(receivedEvent.getParticipantContextId(), receivedEvent.getIssuer(), typesAndFormats);
            if (openDuplicate.isPresent()) {
                monitor.info("Credential offer with ID = '%s' matches the open credential request with holderPid = '%s'. No new request is initiated.".formatted(receivedEvent.getId(), openDuplicate.get().getHolderPid()));
                storedOffer.transition(CredentialOfferStatus.PROCESSED);
                save(storedOffer);
                return;
            }

            var initiateResult = requestManager.initiateRequest(receivedEvent.getParticipantContextId(), receivedEvent.getIssuer(), getHolderPid(), typesAndFormats);
            // failed credential requests transition to REJECTED rather than PROCESSED
            if (initiateResult.failed()) {
                monitor.warning("Could not initiate credential request after receiving a Credential Offer. Manual Reconciliation is required. Details: %s".formatted(initiateResult.getFailureDetail()));
                storedOffer.transition(CredentialOfferStatus.REJECTED);
            } else {
                storedOffer.transition(CredentialOfferStatus.PROCESSED);
            }

            save(storedOffer);
        });
    }

    private void save(CredentialOffer offer) {
        try {
            credentialOfferStore.save(offer);
        } catch (EdcPersistenceException ex) {
            monitor.warning("Could not persist CredentialOffer in database: %s".formatted(ex.getMessage()));
        }
    }

    /**
     * Finds an open {@link HolderCredentialRequest} of the participant to the given issuer, that requests exactly the same set of credential types and formats.
     */
    private Optional<HolderCredentialRequest> findOpenDuplicate(String participantContextId, String issuerDid, List<RequestedCredential> typesAndFormats) {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("participantContextId", "=", participantContextId))
                .filter(criterion("issuerDid", "=", issuerDid))
                .filter(criterion("state", "in", List.of(CREATED.code(), REQUESTING.code(), REQUESTED.code())))
                .limit(Integer.MAX_VALUE)
                .build();
        var offered = typesAndFormats(typesAndFormats);
        return holderCredentialRequestStore.query(query).stream()
                .filter(request -> typesAndFormats(request.getIdsAndFormats()).equals(offered))
                .findFirst();
    }

    private Set<RequestedCredential> typesAndFormats(List<RequestedCredential> requestedCredentials) {
        return requestedCredentials.stream()
                .map(rc -> new RequestedCredential(null, rc.credentialType(), rc.format()))
                .collect(toSet());
    }

    private List<RequestedCredential> extractTypes(CredentialOffer credentialOffer) {
        List<RequestedCredential> typesAndFormats = new ArrayList<>();

        credentialOffer
                .getCredentialObjects()
                .forEach(credentialObject -> {
                    var type = credentialObject.getCredentialType();
                    var format = ofNullable(dcpProfileRegistry.getProfile(credentialObject.getProfile()))
                            .map(dcpProfile -> dcpProfile.format().name());
                    if (format.isEmpty()) {
                        monitor.warning("Credential offer for '%s': no credential format could be derived from any of the offered profiles: %s".formatted(type, credentialObject.getProfile()));
                    } else {
                        typesAndFormats.add(new RequestedCredential(credentialObject.getId(), type, format.get()));
                    }
                });
        return typesAndFormats;
    }

    private String getHolderPid() {
        return UUID.randomUUID().toString(); //todo: should this be extensible?
    }
}
