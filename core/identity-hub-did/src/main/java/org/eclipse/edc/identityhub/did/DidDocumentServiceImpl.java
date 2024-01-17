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
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identithub.did.spi.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identithub.did.spi.model.DidResource;
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

    @Override
    public ServiceResult<Void> addService(String did, Service service) {
        return transactionContext.execute(() -> {
            var didResource = didResourceStore.findById(did);
            if (didResource == null) {
                return ServiceResult.notFound("DID '%s' not found.".formatted(did));
            }
            var services = didResource.getDocument().getService();
            if (services.stream().anyMatch(s -> s.getId().equals(service.getId()))) {
                return ServiceResult.conflict("DID '%s' already contains a service endpoint with ID '%s'.".formatted(did, service.getId()));
            }
            services.add(service);
            var updateResult = didResourceStore.update(didResource);
            return updateResult.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(updateResult);

        });
    }

    @Override
    public ServiceResult<Void> replaceService(String did, Service service) {
        return transactionContext.execute(() -> {
            var didResource = didResourceStore.findById(did);
            if (didResource == null) {
                return ServiceResult.notFound("DID '%s' not found.".formatted(did));
            }
            var services = didResource.getDocument().getService();
            if (services.stream().noneMatch(s -> s.getId().equals(service.getId()))) {
                return ServiceResult.badRequest("DID '%s' does not contain a service endpoint with ID '%s'.".formatted(did, service.getId()));
            }
            services.add(service);
            var updateResult = didResourceStore.update(didResource);
            return updateResult.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(updateResult);

        });
    }

    @Override
    public ServiceResult<Void> removeService(String did, String serviceId) {
        return transactionContext.execute(() -> {
            var didResource = didResourceStore.findById(did);
            if (didResource == null) {
                return ServiceResult.notFound("DID '%s' not found.".formatted(did));
            }
            var services = didResource.getDocument().getService();
            var hasRemoved = services.removeIf(s -> s.getId().equals(serviceId));
            if (!hasRemoved) {
                return ServiceResult.badRequest("DID '%s' does not contain a service endpoint with ID '%s'.".formatted(did, serviceId));
            }
            var updateResult = didResourceStore.update(didResource);
            return updateResult.succeeded() ?
                    ServiceResult.success() :
                    ServiceResult.fromFailure(updateResult);

        });
    }
}
