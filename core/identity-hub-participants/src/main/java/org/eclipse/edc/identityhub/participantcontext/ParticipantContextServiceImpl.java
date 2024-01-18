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

import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.RandomStringGenerator;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Default implementation of the {@link ParticipantContextService}. Uses a {@link Vault} to store API tokens and a {@link RandomStringGenerator}
 * to generate API tokens. Please use a generator that produces Strings of a reasonable length.
 * <p>
 * This service is transactional.
 */
public class ParticipantContextServiceImpl implements ParticipantContextService {

    private final ParticipantContextStore participantContextStore;
    private final Vault vault;
    private final TransactionContext transactionContext;
    private final RandomStringGenerator tokenGenerator;

    public ParticipantContextServiceImpl(ParticipantContextStore participantContextStore, Vault vault, TransactionContext transactionContext, RandomStringGenerator tokenGenerator) {
        this.participantContextStore = participantContextStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public ServiceResult<Void> createParticipantContext(ParticipantContext context) {
        return transactionContext.execute(() -> {
            var storeRes = participantContextStore.create(context);
            return storeRes.succeeded() ?
                    success() :
                    fromFailure(storeRes);
        });
    }

    @Override
    public ServiceResult<ParticipantContext> getParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var res = participantContextStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("participantContext", "=", participantId)).build());
            if (res.succeeded()) {
                return res.getContent().findFirst()
                        .map(ServiceResult::success)
                        .orElse(notFound("No ParticipantContext with ID '%s' was found.".formatted(participantId)));
            }
            return fromFailure(res);
        });
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var res = participantContextStore.deleteById(participantId);
            return res.succeeded() ? ServiceResult.success() : ServiceResult.fromFailure(res);
        });
    }

    @Override
    public ServiceResult<String> regenerateApiToken(String participantId) {
        return transactionContext.execute(() -> {
            var participantContext = getParticipantContext(participantId);
            if (participantContext.failed()) {
                return participantContext.map(pc -> null);
            }
            var alias = participantContext.getContent().getApiTokenAlias();

            var newToken = tokenGenerator.generate();
            return vault.storeSecret(alias, newToken).map(unused -> ServiceResult.success(newToken)).orElse(f -> ServiceResult.conflict("Could not store new API token: %s.".formatted(f.getFailureDetail())));
        });
    }
}


