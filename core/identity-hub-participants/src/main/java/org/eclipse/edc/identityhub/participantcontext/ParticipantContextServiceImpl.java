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
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

    public ParticipantContextServiceImpl(ParticipantContextStore participantContextStore, DidResourceStore didResourceStore, Vault vault, TransactionContext transactionContext, ParticipantContextObservable observable) {
        this.participantContextStore = participantContextStore;
        this.didResourceStore = didResourceStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.tokenGenerator = new ApiTokenGenerator();
    }

    @Override
    public ServiceResult<String> createParticipantContext(ParticipantManifest manifest) {
        return transactionContext.execute(() -> {
            if (didResourceStore.findById(manifest.getDid()) != null) {
                return ServiceResult.conflict("Another participant with the same DID '%s' already exists.".formatted(manifest.getDid()));
            }
            var apiKey = new AtomicReference<String>();
            var context = convert(manifest);
            var res = createParticipantContext(context)
                    .compose(u -> createTokenAndStoreInVault(context)).onSuccess(apiKey::set)
                    .onSuccess(apiToken -> observable.invokeForEach(l -> l.created(context, manifest)));
            return res.map(u -> apiKey.get());
        });
    }

    @Override
    public ServiceResult<ParticipantContext> getParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var res = participantContextStore.query(ParticipantResource.queryByParticipantId(participantId).build());
            if (res.succeeded()) {
                return res.getContent().stream().findFirst()
                        .map(ServiceResult::success)
                        .orElse(notFound("ParticipantContext with ID '%s' does not exist.".formatted(participantId)));
            }
            return fromFailure(res);
        });
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var participantContext = findByIdInternal(participantId);
            if (participantContext == null) {
                return ServiceResult.notFound("A ParticipantContext with ID '%s' does not exist.");
            }

            var res = participantContextStore.deleteById(participantId);
            if (res.failed()) {
                return fromFailure(res);
            }

            observable.invokeForEach(l -> l.deleted(participantContext));
            return ServiceResult.success();
        });
    }

    @Override
    public ServiceResult<String> regenerateApiToken(String participantId) {
        return transactionContext.execute(() -> {
            var participantContext = getParticipantContext(participantId);
            if (participantContext.failed()) {
                return participantContext.map(pc -> null);
            }
            return createTokenAndStoreInVault(participantContext.getContent());
        });
    }

    @Override
    public ServiceResult<Void> updateParticipant(String participantId, Consumer<ParticipantContext> modificationFunction) {
        return transactionContext.execute(() -> {
            var participant = findByIdInternal(participantId);
            if (participant == null) {
                return notFound("ParticipantContext with ID '%s' not found.".formatted(participantId));
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
        var newToken = tokenGenerator.generate(participantContext.getParticipantId());
        return vault.storeSecret(alias, newToken)
                .map(unused -> success(newToken))
                .orElse(f -> conflict("Could not store new API token: %s.".formatted(f.getFailureDetail())));
    }


    private ServiceResult<Void> createParticipantContext(ParticipantContext context) {
        var storeRes = participantContextStore.create(context);
        return storeRes.succeeded() ?
                success() :
                fromFailure(storeRes);
    }

    private ParticipantContext findByIdInternal(String participantId) {
        var resultStream = participantContextStore.query(ParticipantResource.queryByParticipantId(participantId).build());
        if (resultStream.failed()) return null;
        return resultStream.getContent().stream().findFirst().orElse(null);
    }


    private ParticipantContext convert(ParticipantManifest manifest) {
        return ParticipantContext.Builder.newInstance()
                .participantId(manifest.getParticipantId())
                .roles(manifest.getRoles())
                .did(manifest.getDid())
                .apiTokenAlias("%s-%s".formatted(manifest.getParticipantId(), API_KEY_ALIAS_SUFFIX))
                .state(manifest.isActive() ? ParticipantContextState.ACTIVATED : ParticipantContextState.CREATED)
                .build();
    }
}
