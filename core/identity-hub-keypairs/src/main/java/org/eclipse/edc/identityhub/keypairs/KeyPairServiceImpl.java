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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEventListener;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.ACTIVATED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.CREATED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.result.ServiceResult.success;

public class KeyPairServiceImpl implements KeyPairService, EventSubscriber {
    private final KeyPairResourceStore keyPairResourceStore;
    private final Vault vault;
    private final Monitor monitor;
    private final KeyPairObservable observable;
    private final TransactionContext transactionContext;
    private final ParticipantContextStore participantContextService;

    public KeyPairServiceImpl(KeyPairResourceStore keyPairResourceStore, Vault vault, Monitor monitor, KeyPairObservable observable, TransactionContext transactionContext, ParticipantContextStore participantContextService) {
        this.keyPairResourceStore = keyPairResourceStore;
        this.vault = vault;
        this.monitor = monitor;
        this.observable = observable;
        this.transactionContext = transactionContext;
        this.participantContextService = participantContextService;
    }

    @Override
    @WithSpan(value = "keypairs.add", kind = SpanKind.INTERNAL)
    public ServiceResult<Void> addKeyPair(String participantContextId, KeyDescriptor keyDescriptor, boolean makeDefault) {

        return transactionContext.execute(() -> {

            var result = checkParticipantState(participantContextId, ACTIVATED, CREATED)
                    .compose(v -> checkKeyIdAvailable(participantContextId, keyDescriptor.getKeyId(), null));
            if (result.failed()) {
                return result;
            }

            if (!keyDescriptor.isActive()) {
                warnIfNoActiveKeyPair(participantContextId);
            }

            // a failure is returned rather than thrown, so that it does not roll back a surrounding transaction, e.g. the one
            // that creates the participant context. Nothing is written to the database when it happens.
            return storePrivateKey(participantContextId, keyDescriptor)
                    .compose(newKey -> createKeyPair(newKey, makeDefault))
                    .onSuccess(this::announceAdded)
                    .mapEmpty();
        });
    }

    @Override
    public ServiceResult<Void> rotateKeyPair(String oldId, @Nullable KeyDescriptor newKeyDesc, long duration) {
        if (newKeyDesc == null) {
            monitor.warning("Rotating keys without a successor key may leave the participant without an active keypair.");
        }
        // a rotated key stays in the DID document, so its successor cannot have the same key ID
        return replaceKeyPair(oldId, newKeyDesc, false, oldKey -> oldKey.rotate(duration), (listener, oldKey) -> listener.rotated(oldKey, newKeyDesc));
    }

    @Override
    public ServiceResult<Void> revokeKey(String id, @Nullable KeyDescriptor newKeyDesc) {
        if (newKeyDesc == null) {
            monitor.warning("Revoking keys without a successor key may leave the participant without an active keypair.");
        }
        // a revoked key is removed from the DID document, so its successor may have the same key ID
        return replaceKeyPair(id, newKeyDesc, true, KeyPairResource::revoke, (listener, oldKey) -> listener.revoked(oldKey, newKeyDesc));
    }

    @Override
    public ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
        return ServiceResult.from(keyPairResourceStore.query(querySpec));
    }

    @Override
    public ServiceResult<Void> activate(String keyPairResourceId) {
        return transactionContext.execute(() -> {
            var existingKeyPair = findById(keyPairResourceId);
            if (existingKeyPair == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(keyPairResourceId));
            }
            if (existingKeyPair.getState() == KeyPairState.ACTIVATED.code()) {
                // the key pair is in the DID document already, activating it again must not add it a second time
                return success();
            }

            return activateKeyPair(existingKeyPair);
        });
    }

    @Override
    public ServiceResult<KeyPairResource> getActiveKeyPairForUsage(String participantContextId, KeyPairUsage usage) {
        return transactionContext.execute(() -> {
            var query = queryByParticipantContextId(participantContextId)
                    .filter(new Criterion("state", "=", KeyPairState.ACTIVATED.code()))
                    .build();


            var keyPairResult = keyPairResourceStore.query(query);
            if (keyPairResult.failed()) {
                return ServiceResult.unexpected("Error obtaining private key for participant '%s': %s".formatted(participantContextId, keyPairResult.getFailureDetail()));
            }


            var keyPairs = keyPairResult.getContent().stream().filter(kp -> kp.getUsage().contains(usage)).toList();
            // check if there is a default key pair
            ServiceResult<KeyPairResource> selectedKeyPairResult;
            if (keyPairs.size() > 1) {
                selectedKeyPairResult = keyPairs.stream()
                        .filter(KeyPairResource::isDefaultPair)
                        .findAny()
                        .map(ServiceResult::success) // find the default key
                        .orElse(ServiceResult.badRequest("Multiple key-pairs found for signing credentials, but none was marked as 'default'"));
            } else { //skip check for
                selectedKeyPairResult = keyPairs.stream().findFirst()
                        .map(ServiceResult::success)
                        .orElse(ServiceResult.notFound("No active key pair found for participant '%s' with usage '%s'".formatted(participantContextId, usage.name())));
            }

            return selectedKeyPairResult;
        });
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var payload = eventEnvelope.getPayload();
        if (payload instanceof ParticipantContextDeleted deleted) {
            deleted(deleted);
        } else {
            monitor.warning("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }
    }

    /**
     * checks if the participant exists, and that its {@link IdentityHubParticipantContext#getState()} flag matches either of the given states
     *
     * @param participantContextId the ParticipantContext ID of the participant context
     * @param allowedStates        a (possible empty) list of allowed states a participant may be in for a particular operation.
     * @return {@link ServiceResult#success()} if the participant context exists, and is in one of the allowed states, a failure otherwise.
     */
    private ServiceResult<Void> checkParticipantState(String participantContextId, ParticipantContextState... allowedStates) {
        return ServiceResult.from(participantContextService.findById(participantContextId))
                .compose(participantContext -> {
                    var state = participantContext.getStateAsEnum();
                    if (!Arrays.asList(allowedStates).contains(state)) {
                        return ServiceResult.badRequest("To add a key pair, the ParticipantContext with ID '%s' must be in state %s or %s but was %s."
                                .formatted(participantContextId, ACTIVATED, CREATED, state));
                    }
                    return success();
                });
    }

    private @NotNull ServiceResult<Void> activateKeyPair(KeyPairResource existingKeyPair) {
        var allowedStates = List.of(KeyPairState.ACTIVATED.code(), KeyPairState.CREATED.code());
        if (!allowedStates.contains(existingKeyPair.getState())) {
            return ServiceResult.badRequest("The key pair resource is expected to be in %s, but was %s".formatted(allowedStates, existingKeyPair.getState()));
        }
        existingKeyPair.activate();

        return ServiceResult.from(keyPairResourceStore.update(existingKeyPair)
                .onSuccess(u -> observable.invokeForEach(l -> l.activated(existingKeyPair, existingKeyPair.getKeyContext()))));
    }

    private void deleted(ParticipantContextDeleted event) {
        //hard-delete all keypairs that are associated with the deleted participant
        var query = queryByParticipantContextId(event.getParticipantContextId()).build();
        transactionContext.execute(() -> {
            keyPairResourceStore.query(query)
                    .compose(list -> {
                        var errors = list.stream()
                                .map(r -> keyPairResourceStore.deleteById(r.getId()))
                                .filter(StoreResult::failed)
                                .map(AbstractResult::getFailureDetail)
                                .collect(Collectors.joining(","));

                        if (errors.isEmpty()) {
                            return StoreResult.success();
                        }
                        return StoreResult.generalError("An error occurred when deleting KeyPairResources: %s".formatted(errors));
                    })
                    .onFailure(f -> monitor.warning("Removing key pairs from a deleted ParticipantContext failed: %s".formatted(f.getFailureDetail())));
        });
    }

    /**
     * A key ID identifies the verification method of its key pair in the DID document, so it must not be shared with another
     * key pair of the participant context that is, or may become, part of the DID document, i.e. one that is not revoked.
     *
     * @param exceptKeyPairId the ID of a key pair that is about to be revoked, so that its key ID may be reused, or null
     * @return A successful result if the key ID is available, or an error result otherwise
     */
    private ServiceResult<Void> checkKeyIdAvailable(String participantContextId, String keyId, @Nullable String exceptKeyPairId) {
        var query = queryByParticipantContextId(participantContextId)
                .filter(new Criterion("keyId", "=", keyId))
                .build();
        var existingKeyPairs = keyPairResourceStore.query(query);
        if (existingKeyPairs.failed()) {
            return ServiceResult.fromFailure(existingKeyPairs);
        }
        var inUse = existingKeyPairs.getContent().stream()
                .filter(keyPair -> !keyPair.getId().equals(exceptKeyPairId))
                .anyMatch(keyPair -> keyPair.getState() != KeyPairState.REVOKED.code());
        return inUse
                ? ServiceResult.conflict("A key pair with key ID '%s' already exists for participant context '%s'.".formatted(keyId, participantContextId))
                : success();
    }

    private KeyPairResource findById(String oldId) {
        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", oldId)).build();
        return keyPairResourceStore.query(q).map(list -> list.stream().findFirst().orElse(null)).orElse(f -> null);
    }

    /**
     * Takes a key pair out of use, by rotating or revoking it, and adds its successor, if one is given.
     * <p>
     * The vault is not part of the transaction, so its changes are ordered around it:
     * <ul>
     *     <li>The successor's private key is stored before anything is written to the database, and before any event takes
     *     locks, e.g. on the DID document. It is deleted again if the successor is not added after all.</li>
     *     <li>The successor and the old key pair are written in the same transaction, which is rolled back if either of them
     *     fails, so that the old key pair is never taken out of use without its successor.</li>
     *     <li>The events are only emitted once both are written, the old key pair's first: a revoked key pair is removed
     *     from the DID document, and its successor may have the same key ID.</li>
     *     <li>The old private key is only deleted from the vault once the transaction is completed, so that a failure leaves
     *     the old key pair usable.</li>
     * </ul>
     *
     * @param successorMayReuseKeyId whether the successor may have the key ID of the old key pair
     * @param takeOutOfUse           rotates or revokes the old key pair
     * @param announce               emits the event about the old key pair
     */
    private ServiceResult<Void> replaceKeyPair(String oldId, @Nullable KeyDescriptor successorDescriptor, boolean successorMayReuseKeyId,
                                               Consumer<KeyPairResource> takeOutOfUse, BiConsumer<KeyPairEventListener, KeyPairResource> announce) {
        ServiceResult<Replacement> result;
        try {
            result = transactionContext.execute(() -> {
                var oldKey = findById(oldId);
                if (oldKey == null) {
                    return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(oldId));
                }
                // the private key of a key pair that was rotated or revoked before is deleted already
                var replacement = new Replacement(oldKey, !wasTakenOutOfUse(oldKey));
                // the successor takes over the default flag, which is reset when the old key pair is taken out of use
                var wasDefault = oldKey.isDefaultPair();
                takeOutOfUse.accept(oldKey);

                if (successorDescriptor == null) {
                    return ServiceResult.from(keyPairResourceStore.update(oldKey))
                            .onSuccess(v -> observable.invokeForEach(l -> announce.accept(l, oldKey)))
                            .map(v -> replacement);
                }

                // the successor is checked before anything is written, so that a rejected successor does not leave the old
                // key pair out of use
                var participantContextId = oldKey.getParticipantContextId();
                var check = checkParticipantState(participantContextId, ACTIVATED, CREATED)
                        .compose(v -> checkKeyIdAvailable(participantContextId, successorDescriptor.getKeyId(), successorMayReuseKeyId ? oldKey.getId() : null));
                if (check.failed()) {
                    return check.mapFailure();
                }

                return storePrivateKey(participantContextId, successorDescriptor)
                        .compose(newKey -> writeReplacement(oldKey, newKey, wasDefault))
                        .onSuccess(successor -> {
                            // the outgoing old key pair is announced first: a revoked key pair is removed from the DID document, and
                            // its successor may have the same key ID
                            observable.invokeForEach(l -> announce.accept(l, oldKey));
                            announceAdded(successor);
                            if (!successorDescriptor.isActive()) {
                                warnIfNoActiveKeyPair(participantContextId);
                            }
                        })
                        .map(v -> replacement);
            });
        } catch (RollbackException e) {
            return e.failure();
        }

        return result
                .onSuccess(replacement -> {
                    if (replacement.deletePrivateKey()) {
                        deletePrivateKey(replacement.oldKey());
                    }
                })
                .mapEmpty();
    }

    /**
     * Writes the successor, and the old key pair, which was taken out of use. Once the successor is written, a failure
     * throws a {@link RollbackException}, because the transaction is only rolled back when an exception is thrown.
     */
    private ServiceResult<KeyPairResource> writeReplacement(KeyPairResource oldKey, NewKey newKey, boolean makeDefault) {
        var successor = createKeyPair(newKey, makeDefault);
        if (successor.failed()) {
            return successor;
        }

        StoreResult<Void> updateResult;
        try {
            updateResult = keyPairResourceStore.update(oldKey);
        } catch (RuntimeException e) {
            discardPrivateKey(newKey);
            throw e;
        }
        if (updateResult.failed()) {
            discardPrivateKey(newKey);
            throw new RollbackException(ServiceResult.fromFailure(updateResult));
        }
        return successor;
    }

    /**
     * Stores the private key of a key pair that is generated here, or takes the public key of one whose private key was
     * stored in the vault beforehand.
     * <p>
     * The vault is not part of the transaction, so this happens before anything is written to the database, and the private
     * key is deleted again if the key pair is not added after all, c.f. {@link #discardPrivateKey(NewKey)}. An existing
     * secret is never overwritten: it may be the private key of another key pair, or any other secret of the participant
     * context.
     */
    private ServiceResult<NewKey> storePrivateKey(String participantContextId, KeyDescriptor keyDescriptor) {
        if (keyDescriptor.getKeyGeneratorParams() == null) {
            // either take the public key from the JWK structure or the PEM field
            var publicKeySerialized = Optional.ofNullable(keyDescriptor.getPublicKeyJwk())
                    .map(m -> CryptoConverter.create(m).toJSONString())
                    .orElseGet(() -> keyDescriptor.getPublicKeyPem().replace("\\n", "\n"));
            return success(new NewKey(participantContextId, keyDescriptor, publicKeySerialized, false));
        }

        var alias = keyDescriptor.getPrivateKeyAlias();
        if (vault.resolveSecret(participantContextId, alias) != null) {
            return ServiceResult.conflict("A secret with alias '%s' already exists for participant context '%s'.".formatted(alias, participantContextId));
        }

        var keyPair = KeyPairGenerator.generateKeyPair(keyDescriptor.getKeyGeneratorParams());
        if (keyPair.failed()) {
            return ServiceResult.badRequest(keyPair.getFailureDetail());
        }
        var privateJwk = CryptoConverter.createJwk(keyPair.getContent(), keyDescriptor.getKeyId());
        var storeResult = vault.storeSecret(participantContextId, alias, privateJwk.toJSONString());
        if (storeResult.failed()) {
            return ServiceResult.unexpected("Failed to store the private key with alias '%s': %s".formatted(alias, storeResult.getFailureDetail()));
        }
        return success(new NewKey(participantContextId, keyDescriptor, privateJwk.toPublicJWK().toJSONString(), true));
    }

    /**
     * Writes a new key pair. If that fails, its private key is deleted from the vault again.
     */
    private ServiceResult<KeyPairResource> createKeyPair(NewKey newKey, boolean makeDefault) {
        var keyDescriptor = newKey.descriptor();
        var keyPair = KeyPairResource.Builder.newInstance()
                .usage(keyDescriptor.getUsage())
                .id(keyDescriptor.getResourceId())
                .keyId(keyDescriptor.getKeyId())
                .state(keyDescriptor.isActive() ? KeyPairState.ACTIVATED : KeyPairState.CREATED)
                .isDefaultPair(makeDefault)
                .privateKeyAlias(keyDescriptor.getPrivateKeyAlias())
                .serializedPublicKey(newKey.publicKeySerialized())
                .timestamp(Instant.now().toEpochMilli())
                .participantContextId(newKey.participantContextId())
                .keyContext(keyDescriptor.getType())
                .build();

        StoreResult<Void> createResult;
        try {
            createResult = keyPairResourceStore.create(keyPair);
        } catch (RuntimeException e) {
            discardPrivateKey(newKey);
            throw e;
        }
        if (createResult.failed()) {
            discardPrivateKey(newKey);
            return ServiceResult.fromFailure(createResult);
        }
        return success(keyPair);
    }

    private void announceAdded(KeyPairResource keyPair) {
        observable.invokeForEach(l -> {
            l.added(keyPair, keyPair.getKeyContext());
            // downstream services only take up an active key pair, e.g. into the DID document, once it is announced as activated
            if (keyPair.getState() == KeyPairState.ACTIVATED.code()) {
                l.activated(keyPair, keyPair.getKeyContext());
            }
        });
    }

    private void warnIfNoActiveKeyPair(String participantContextId) {
        var hasActiveKeyPair = keyPairResourceStore.query(queryByParticipantContextId(participantContextId).build())
                .orElse(failure -> Collections.emptySet())
                .stream()
                .anyMatch(kpr -> kpr.getState() == KeyPairState.ACTIVATED.code());

        if (!hasActiveKeyPair) {
            monitor.warning("Participant '%s' has no active key pairs, and adding an inactive one will prevent the participant from becoming operational."
                    .formatted(participantContextId));
        }
    }

    private boolean wasTakenOutOfUse(KeyPairResource keyPair) {
        return keyPair.getState() == KeyPairState.ROTATED.code() || keyPair.getState() == KeyPairState.REVOKED.code();
    }

    /**
     * Deletes the private key of a key pair that is not added after all.
     */
    private void discardPrivateKey(NewKey newKey) {
        if (newKey.privateKeyStored()) {
            var alias = newKey.descriptor().getPrivateKeyAlias();
            vault.deleteSecret(newKey.participantContextId(), alias)
                    .onFailure(f -> monitor.warning("Failed to delete the private key '%s' of a key pair that was not added, it must be deleted manually: %s"
                            .formatted(alias, f.getFailureDetail())));
        }
    }

    /**
     * Deletes the private key of a key pair that was taken out of use. This happens once the transaction is completed, so
     * a failure leaves the private key in the vault, where it is never used again.
     */
    private void deletePrivateKey(KeyPairResource keyPair) {
        vault.deleteSecret(keyPair.getParticipantContextId(), keyPair.getPrivateKeyAlias())
                .onFailure(f -> monitor.warning("Failed to delete the private key '%s' of key pair '%s', it must be deleted manually: %s"
                        .formatted(keyPair.getPrivateKeyAlias(), keyPair.getId(), f.getFailureDetail())));
    }

    /**
     * The key material of a key pair that is about to be added.
     *
     * @param privateKeyStored whether its private key was stored in the vault here, and must be deleted again if the key pair is not added
     */
    private record NewKey(String participantContextId, KeyDescriptor descriptor, String publicKeySerialized, boolean privateKeyStored) {
    }

    /**
     * A key pair that was taken out of use.
     *
     * @param deletePrivateKey whether its private key is still in the vault
     */
    private record Replacement(KeyPairResource oldKey, boolean deletePrivateKey) {
    }

    /**
     * Rolls back a replacement of a key pair that could not be written completely, and carries its failure out of the
     * transaction.
     */
    private static class RollbackException extends EdcException {
        private final ServiceResult<Void> failure;

        RollbackException(ServiceResult<Void> failure) {
            super(failure.getFailureDetail());
            this.failure = failure;
        }

        <T> ServiceResult<T> failure() {
            return failure.mapFailure();
        }
    }
}
