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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.transit.TransitEngine;
import org.eclipse.edc.identityhub.transit.TransitKeyDescriptor;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.ACTIVATED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.CREATED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.result.ServiceResult.success;

public class TransitKeyPairService implements KeyPairService, EventSubscriber {
    public static final String DEFAULT_KEY_TYPE = "ed25519";
    private final KeyPairResourceStore keyPairResourceStore;
    private final Monitor monitor;
    private final KeyPairObservable observable;
    private final TransactionContext transactionContext;
    private final ParticipantContextStore participantContextService;
    private final TransitEngine transitEngine;

    public TransitKeyPairService(KeyPairResourceStore keyPairResourceStore, Monitor monitor, KeyPairObservable observable, TransactionContext transactionContext, ParticipantContextStore participantContextService, TransitEngine transitEngine) {
        this.keyPairResourceStore = keyPairResourceStore;
        this.monitor = monitor;
        this.observable = observable;
        this.transactionContext = transactionContext;
        this.participantContextService = participantContextService;
        this.transitEngine = transitEngine;
    }

    private static @NotNull String generateKeyName(String participantContextId, String keyId) {
        return "participant_" + participantContextId + "_" + keyId;
    }

    @Override
    @WithSpan(value = "keypairs.add", kind = SpanKind.INTERNAL)
    public ServiceResult<Void> addKeyPair(String participantContextId, KeyDescriptor keyDescriptor, boolean makeDefault) {

        if ((keyDescriptor.getPublicKeyJwk() != null && !keyDescriptor.getPublicKeyJwk().isEmpty()) || keyDescriptor.getPublicKeyPem() != null) {
            return ServiceResult.badRequest("Importing externally generated keys into the KeyPairService is not supported.");
        }

        return transactionContext.execute(() -> {

            var result = checkParticipantState(participantContextId);

            if (result.failed()) {
                return result.mapEmpty();
            }

            // generate key using Hashicorp Transit
            var type = ofNullable(keyDescriptor.getKeyGeneratorParams())
                    .orElse(Map.of())
                    .getOrDefault("type", DEFAULT_KEY_TYPE).toString();

            var keyName = generateKeyName(participantContextId, keyDescriptor.getKeyId());
            var keyResult = transitEngine.generateKey(keyName, type);
            if (keyResult.failed()) {
                return ServiceResult.from(keyResult.mapEmpty());
            }

            var publicKey = keyResult.getContent().getLatestVersion();
            if (publicKey.failed()) {
                return ServiceResult.from(publicKey.mapEmpty());
            }

            var newResource = KeyPairResource.Builder.newInstance()
                    .usage(keyDescriptor.getUsage())
                    .id(keyDescriptor.getResourceId())
                    .keyId(keyDescriptor.getKeyId())
                    .state(KeyPairState.ACTIVATED)
                    .isDefaultPair(makeDefault)
                    .privateKeyAlias(keyName)
                    .serializedPublicKey(publicKey.getContent().getPublicKey())
                    .timestamp(Instant.now().toEpochMilli())
                    .participantContextId(participantContextId)
                    .keyContext(keyDescriptor.getType())
                    .build();

            return ServiceResult.from(keyPairResourceStore.create(newResource))
                    .onSuccess(v -> observable.invokeForEach(l -> l.added(newResource, keyDescriptor.getType())));
        });
    }

    @Override
    @WithSpan(value = "keypairs.rotate", kind = SpanKind.INTERNAL)
    public ServiceResult<Void> rotateKeyPair(String oldId, @Nullable KeyDescriptor newKeyDesc, long duration) {
        if (newKeyDesc != null) {
            monitor.warning("Supplying new key parameters is not supported when rotating keys managed the Transit Engine. " +
                    "Parameters will be ignored.");
        }

        return transactionContext.execute(() -> {
            var oldKey = findById(oldId);
            if (oldKey == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(oldId));
            }

            var participantContextId = oldKey.getParticipantContextId();

            // deactivate the old key
            oldKey.rotate(duration);

            var res = keyPairResourceStore.update(oldKey);
            if (res.failed()) {
                return ServiceResult.from(res);
            }

            // have Transit rotate the key, and create a copy of the keypairResource

            var keyName = generateKeyName(participantContextId, oldKey.getKeyId());
            var rotateResult = transitEngine.rotateKey(keyName)
                    .compose(u -> transitEngine.getKey(keyName))
                    .compose(tkd -> transitEngine.setMinEncryptionKeyVersion(keyName, tkd.getData().getLatestVersion()).compose(u -> Result.success(tkd)))
                    .compose(TransitKeyDescriptor::getLatestVersion)
                    .map(keyVersion -> KeyPairResource.Builder.newInstance()
                            .usage(oldKey.getUsage())
                            .id(oldKey.getId())
                            .keyId(oldKey.getKeyId())
                            .state(KeyPairState.ACTIVATED)
                            .isDefaultPair(true)
                            .privateKeyAlias(keyName)
                            .serializedPublicKey(keyVersion.getPublicKey())
                            .timestamp(Instant.now().toEpochMilli())
                            .participantContextId(participantContextId)
                            .keyContext(oldKey.getKeyContext())
                            .build())
                    .map(keyPairResourceStore::create)
                    .onSuccess(v -> observable.invokeForEach(l -> l.rotated(oldKey, newKeyDesc)));
            return rotateResult.succeeded() ? ServiceResult.success() : ServiceResult.badRequest(rotateResult.getFailureDetail());
        });
    }

    @Override
    @WithSpan(value = "keypairs.revoke", kind = SpanKind.INTERNAL)
    public ServiceResult<Void> revokeKey(String id, @Nullable KeyDescriptor newKeyDesc) {
        return transactionContext.execute(() -> {
            if (newKeyDesc != null) {
                monitor.warning("Supplying new parameters is not supported when rotating keys using the Transit Engine.");
            }

            var oldKey = findById(id);
            if (oldKey == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(id));
            }

            var participantContextId = oldKey.getParticipantContextId();

            // mark the old key as "revoked"
            oldKey.revoke();
            var res = keyPairResourceStore.update(oldKey);
            if (res.failed()) {
                return ServiceResult.from(res);
            }

            // there is no "revoke" action in Transit, so we rotate the key and trim to the latest version
            var keyName = generateKeyName(participantContextId, oldKey.getKeyId());
            var transitResult = transitEngine.rotateKey(keyName)
                    .compose(u -> transitEngine.getKey(keyName))
                    .compose(tkd -> transitEngine.setMinAvailableVersion(keyName, tkd.getData().getLatestVersion())
                            .compose(u -> transitEngine.setMinEncryptionKeyVersion(keyName, tkd.getData().getLatestVersion()))
                            .compose(u -> transitEngine.setMinDecryptionKeyVersion(keyName, tkd.getData().getLatestVersion()))
                            .compose(u -> Result.success(tkd)))
                    .compose(TransitKeyDescriptor::getLatestVersion)
                    .map(latestVersion ->
                            KeyPairResource.Builder.newInstance()
                                    .usage(oldKey.getUsage())
                                    .id(oldKey.getId())
                                    .keyId(oldKey.getKeyId())
                                    .state(KeyPairState.ACTIVATED)
                                    .isDefaultPair(true)
                                    .privateKeyAlias(keyName)
                                    .serializedPublicKey(latestVersion.getPublicKey())
                                    .timestamp(Instant.now().toEpochMilli())
                                    .participantContextId(participantContextId)
                                    .keyContext(oldKey.getKeyContext())
                                    .build());

            // ... and finally create the new keypair resource
            if (transitResult.succeeded()) {
                return ServiceResult.from(keyPairResourceStore.create(transitResult.getContent()))
                        .onSuccess(v -> observable.invokeForEach(l -> l.revoked(oldKey, newKeyDesc)));
            }
            return ServiceResult.from(transitResult.mapFailure());

        });
    }

    @Override
    public ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
        return ServiceResult.from(keyPairResourceStore.query(querySpec));
    }

    @Override
    public ServiceResult<Void> activate(String keyPairResourceId) {
        monitor.warning("This implementation uses the Vault Transit Engine, which does not support activation." +
                "Performing this action will have no effect on the key material, only on the resource stored in the database.");
        return transactionContext.execute(() -> {
            var existingKeyPair = findById(keyPairResourceId);
            if (existingKeyPair == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(keyPairResourceId));
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
     * @return {@link ServiceResult#success()} if the participant context exists, and is in one of the allowed states, a failure otherwise.
     */
    private ServiceResult<Void> checkParticipantState(String participantContextId) {
        var result = ServiceResult.from(participantContextService.query(queryByParticipantContextId(participantContextId).build()))
                .compose(list -> list.stream().findFirst()
                        .map(pc -> {
                            var state = pc.getStateAsEnum();
                            if (!Arrays.asList(new ParticipantContextState[]{ ParticipantContextState.ACTIVATED, ParticipantContextState.CREATED }).contains(state)) {
                                return ServiceResult.badRequest("To add a key pair, the ParticipantContext with ID '%s' must be in state %s or %s but was %s."
                                        .formatted(participantContextId, ACTIVATED, CREATED, state));
                            }
                            return success();
                        })
                        .orElse(ServiceResult.notFound("No ParticipantContext with ID '%s' was found.".formatted(participantContextId))));
        return result.mapEmpty();
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

    private KeyPairResource findById(String oldId) {
        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", oldId)).build();
        return keyPairResourceStore.query(q).map(list -> list.stream().findFirst().orElse(null)).orElse(f -> null);
    }
}
