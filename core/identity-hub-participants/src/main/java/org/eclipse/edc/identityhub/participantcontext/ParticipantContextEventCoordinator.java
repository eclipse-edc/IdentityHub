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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleting;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Coordinates {@link ParticipantContextCreated} events. More specifically, it coordinates the sequence, in which the following actions are performed:
 * <ul>
 *     <li>Create DID Document</li>
 *     <li>Optional: publish DID document</li>
 *     <li>Add a KeyPair</li>
 * </ul>
 * To that end, the {@link ParticipantContextEventCoordinator} directly collaborates with the {@link KeyPairService} and the {@link DidDocumentService}.
 * <p>
 * Please note that once this initial sequence is executed, every collaborator service emits their events as per their event contract.
 * For example, once a KeyPair is added, the {@link KeyPairService} will emit a {@link org.eclipse.edc.identityhub.spi.events.keypair.KeyPairAdded} event. The {@link DidDocumentService}
 * can then react to this event by updating the DID Document.
 */
class ParticipantContextEventCoordinator implements EventSubscriber {
    private final Monitor monitor;
    private final DidDocumentService didDocumentService;
    private final KeyPairService keyPairService;

    ParticipantContextEventCoordinator(Monitor monitor, DidDocumentService didDocumentService, KeyPairService keyPairService) {
        this.monitor = monitor;
        this.didDocumentService = didDocumentService;
        this.keyPairService = keyPairService;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        if (payload instanceof ParticipantContextCreated createdEvent) {
            var manifest = createdEvent.getManifest();
            var doc = DidDocument.Builder.newInstance()
                    .id(manifest.getDid())
                    .service(manifest.getServiceEndpoints().stream().toList())
                    // updating and adding a verification method happens as a result of the KeyPairAddedEvent
                    .build();

            if (manifest.isActive() && !manifest.getKey().isActive()) {
                monitor.warning("The ParticipantContext is 'active', but its (only) KeyPair is 'inActive'. " +
                        "This will result in a DID Document without Verification Methods, and thus, an unusable ParticipantContext.");
            }

            didDocumentService.store(doc, manifest.getParticipantId())
                    // adding the keypair event will cause the DidDocumentService to update the DID.
                    .compose(u -> keyPairService.addKeyPair(createdEvent.getParticipantId(), createdEvent.getManifest().getKey(), true))
                    .compose(u -> manifest.isActive() ? didDocumentService.publish(doc.getId()) : success())
                    .onFailure(f -> monitor.warning("%s".formatted(f.getFailureDetail())));

        } else if (payload instanceof ParticipantContextDeleting deletionEvent) {
            var participantContext = deletionEvent.getParticipantContext();

            // unpublish and delete did document, remove keypairs
            didDocumentService.unpublish(participantContext.getDid())
                    .compose(u -> didDocumentService.deleteById(participantContext.getDid()))
                    .compose(u -> keyPairService.query(KeyPairResource.queryByParticipantId(participantContext.getParticipantId()).build()))
                    .compose(keyPairs -> keyPairs.stream()
                            .map(r -> keyPairService.revokeKey(r.getId(), null))
                            .reduce(this::merge)
                            .orElse(success()))
                    .onFailure(f -> monitor.warning("Removing key pairs from a deleted ParticipantContext failed: %s".formatted(f.getFailureDetail())));
        } else {
            monitor.warning("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }
    }

    private ServiceResult<Void> merge(ServiceResult<Void> sr1, ServiceResult<Void> sr2) {
        return sr1.succeeded() && sr2.succeeded() ? success() :
                ServiceResult.unexpected(Stream.concat(sr1.getFailureMessages().stream(), sr2.getFailureMessages().stream()).toArray(String[]::new));
    }
}
