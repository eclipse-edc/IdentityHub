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

import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Default implementation of the {@link ParticipantContextService}. Uses a {@link Vault} to store API tokens and a {@link ApiTokenGenerator}
 * to generate API tokens. Please use a generator that produces Strings of a reasonable length.
 * <p>
 * This service is transactional.
 */
public class ParticipantContextServiceImpl implements ParticipantContextService {

    private static final String API_KEY_ALIAS_SUFFIX = "apikey";
    private final ParticipantContextStore participantContextStore;
    private final DidResourceStore didResourceStore;
    private final Vault vault;
    private final TransactionContext transactionContext;
    private final ApiTokenGenerator tokenGenerator;
    private final ParticipantContextObservable observable;
    private final StsAccountProvisioner stsAccountProvisioner;

    public ParticipantContextServiceImpl(ParticipantContextStore participantContextStore,
                                         DidResourceStore didResourceStore,
                                         Vault vault,
                                         TransactionContext transactionContext,
                                         ParticipantContextObservable observable,
                                         StsAccountProvisioner stsAccountProvisioner) {
        this.participantContextStore = participantContextStore;
        this.didResourceStore = didResourceStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.stsAccountProvisioner = stsAccountProvisioner;
        this.tokenGenerator = new ApiTokenGenerator();
    }

    @Override
    public ServiceResult<CreateParticipantContextResponse> createParticipantContext(ParticipantManifest manifest) {
        return transactionContext.execute(() -> {
            if (didResourceStore.findById(manifest.getDid()) != null) {
                return ServiceResult.conflict("Another participant with the same DID '%s' already exists.".formatted(manifest.getDid()));
            }
            var context = convert(manifest);

            return createParticipantContext(context)
                    .compose(this::createTokenAndStoreInVault)
                    .compose((Function<String, ServiceResult<CreateParticipantContextResponse>>) apiKey -> stsAccountProvisioner.create(manifest)
                            .map(accountInfo -> {
                                if (accountInfo == null) {
                                    return new CreateParticipantContextResponse(apiKey, null, null);
                                } else {
                                    return new CreateParticipantContextResponse(apiKey, accountInfo.clientId(), accountInfo.clientSecret());
                                }
                            }))
                    .onSuccess(apiToken -> observable.invokeForEach(l -> l.created(context, manifest)));
        });
    }

    @Override
    public ServiceResult<ParticipantContext> getParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> ServiceResult.from(participantContextStore.findById(participantContextId)));
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> {
            var participantContext = findByIdInternal(participantContextId);
            if (participantContext == null) {
                return ServiceResult.notFound("A ParticipantContext with ID '%s' does not exist.");
            }
            // deactivating the PC must be the first step, because unpublishing DIDs requires the PC to be in the DEACTIVATED state.
            // Unpublishing DIDs happens in callback of the "-Deleting" Event
            return updateParticipant(participantContextId, ParticipantContext::deactivate)
                    .compose(v -> {
                        observable.invokeForEach(l -> l.deleting(participantContext));
                        var res = participantContextStore.deleteById(participantContextId);
                        vault.deleteSecret(participantContext.getApiTokenAlias());
                        if (res.failed()) {
                            return fromFailure(res);
                        }

                        observable.invokeForEach(l -> l.deleted(participantContext));
                        return ServiceResult.success();
                    });
        });
    }

    @Override
    public ServiceResult<String> regenerateApiToken(String participantContextId) {
        return transactionContext.execute(() -> {
            var participantContext = getParticipantContext(participantContextId);
            if (participantContext.failed()) {
                return participantContext.map(pc -> null);
            }
            return createTokenAndStoreInVault(participantContext.getContent());
        });
    }

    @Override
    public ServiceResult<Void> updateParticipant(String participantContextId, Consumer<ParticipantContext> modificationFunction) {
        return transactionContext.execute(() -> {
            var participant = findByIdInternal(participantContextId);
            if (participant == null) {
                return notFound("ParticipantContext with ID '%s' not found.".formatted(participantContextId));
            }
            modificationFunction.accept(participant);
            var res = participantContextStore.update(participant)
                    .onSuccess(u -> observable.invokeForEach(l -> l.updated(participant)));
            return res.succeeded() ? success() : fromFailure(res);
        });

    }

    @Override
    public ServiceResult<Collection<ParticipantContext>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.from(participantContextStore.query(querySpec)));
    }

    private ServiceResult<String> createTokenAndStoreInVault(ParticipantContext participantContext) {
        var alias = participantContext.getApiTokenAlias();
        var newToken = tokenGenerator.generate(participantContext.getParticipantContextId());
        return vault.storeSecret(alias, newToken)
                .map(unused -> success(newToken))
                .orElse(f -> conflict("Could not store new API token: %s.".formatted(f.getFailureDetail())));
    }


    private ServiceResult<ParticipantContext> createParticipantContext(ParticipantContext context) {
        var result = participantContextStore.create(context);
        return ServiceResult.from(result).map(it -> context);
    }

    private ParticipantContext findByIdInternal(String participantContextId) {
        var resultStream = participantContextStore.findById(participantContextId);
        return resultStream.orElse(f -> null);
    }


    private ParticipantContext convert(ParticipantManifest manifest) {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(manifest.getParticipantId())
                .roles(manifest.getRoles())
                .did(manifest.getDid())
                .apiTokenAlias("%s-%s".formatted(manifest.getParticipantId(), API_KEY_ALIAS_SUFFIX))
                .state(ParticipantContextState.CREATED)
                .properties(manifest.getAdditionalProperties())
                .build();
    }
}
