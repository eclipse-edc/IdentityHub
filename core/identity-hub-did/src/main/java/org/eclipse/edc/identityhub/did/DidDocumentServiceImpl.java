/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identithub.did.spi.model.DidResource;
import org.eclipse.edc.identithub.did.spi.model.DidState;
import org.eclipse.edc.identithub.did.spi.store.DidResourceStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;

/**
 * This is an aggregate service to manage CRUD operations of {@link DidDocument}s as well as handle their
 * publishing and un-publishing. All methods are executed transactionally.
 */
public class DidDocumentServiceImpl implements DidDocumentService {

    private final TransactionContext transactionContext;
    private final DidResourceStore didResourceStore;
    private final DidDocumentPublisherRegistry registry;

    public DidDocumentServiceImpl(TransactionContext transactionContext, DidResourceStore didResourceStore, DidDocumentPublisherRegistry registry) {
        this.transactionContext = transactionContext;
        this.didResourceStore = didResourceStore;
        this.registry = registry;
    }

    @Override
    public ServiceResult<Void> store(DidDocument document) {
        return transactionContext.execute(() -> {
            var res = DidResource.Builder.newInstance()
                    .document(document)
                    .did(document.getId())
                    .build();
            var result = didResourceStore.save(res);
            return result.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(result);
        });
    }

    @Override
    public ServiceResult<Void> publish(String did) {
        return transactionContext.execute(() -> {
            var existingDoc = didResourceStore.findById(did);
            if (existingDoc == null) {
                return ServiceResult.notFound(notFoundMessage(did));
            }
            var publisher = registry.getPublisher(did);
            if (publisher == null) {
                return ServiceResult.badRequest(noPublisherFoundMessage(did));
            }
            var publishResult = publisher.publish(did);
            return publishResult.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.badRequest(publishResult.getFailureDetail());

        });
    }

    @Override
    public ServiceResult<Void> unpublish(String did) {
        return transactionContext.execute(() -> {
            var existingDoc = didResourceStore.findById(did);
            if (existingDoc == null) {
                return ServiceResult.notFound(notFoundMessage(did));
            }
            var publisher = registry.getPublisher(did);
            if (publisher == null) {
                return ServiceResult.badRequest(noPublisherFoundMessage(did));
            }
            var publishResult = publisher.unpublish(did);
            return publishResult.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.badRequest(publishResult.getFailureDetail());

        });
    }

    @Override
    public ServiceResult<Void> update(DidDocument document) {
        return transactionContext.execute(() -> {
            // obtain existing resource from storage
            var did = document.getId();
            var existing = didResourceStore.findById(did);
            if (existing == null) {
                return ServiceResult.notFound(notFoundMessage(did));
            }

            //update only the did document
            var updatedResource = DidResource.Builder.newInstance()
                    .document(document)
                    .did(did)
                    .state(existing.getState())
                    .createTimestamp(existing.getCreateTimestamp())
                    .stateTimeStamp(existing.getStateTimestamp())
                    .build();

            var res = didResourceStore.update(updatedResource);
            return res.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(res);
        });
    }

    @Override
    public ServiceResult<Void> deleteById(String did) {
        return transactionContext.execute(() -> {
            var existing = didResourceStore.findById(did);
            if (existing == null) {
                return ServiceResult.notFound(notFoundMessage(did));
            }
            if (existing.getState() == DidState.PUBLISHED.code()) {
                return ServiceResult.conflict("Cannot delete DID '%s' because it is already published. Un-publish first!".formatted(did));
            }
            var res = didResourceStore.deleteById(did);
            return res.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(res);
        });
    }

    @Override
    public ServiceResult<Collection<DidDocument>> queryDocuments(QuerySpec query) {
        return transactionContext.execute(() -> {
            var res = didResourceStore.query(query);
            return ServiceResult.success(res.stream().map(DidResource::getDocument).toList());
        });
    }

    @Override
    public DidResource findById(String did) {
        return transactionContext.execute(() -> didResourceStore.findById(did));
    }
}
