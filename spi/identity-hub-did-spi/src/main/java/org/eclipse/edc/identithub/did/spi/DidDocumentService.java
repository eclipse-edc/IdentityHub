/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.did.spi;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identithub.did.spi.model.DidResource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

/**
 * The {@code DidDocumentService} gives access to a {@link DidDocument} that are held in storage.
 */
public interface DidDocumentService {


    /**
     * Publishes an already existing DID document. Returns a failure if the DID document was not found or cannot be published.
     *
     * @param did The ID of the DID document to publish. Must exist in the database.
     * @return success, or a failure indicating what went wrong.
     */
    ServiceResult<Void> publish(String did);

    /**
     * Un-publishes an already existing DID document. Returns a failure if the DID document was not found or the underlying
     * VDR does not support un-publishing
     *
     * @param did The ID of the DID document to un-publish. Must exist in the database.
     * @return success, or a failure indicating what went wrong.
     */
    ServiceResult<Void> unpublish(String did);


    /**
     * Queries the {@link DidDocument} objects based on the given query specification.
     *
     * @param spec The query
     * @return A {@link ServiceResult} containing a collection of {@link DidDocument} objects that match the query parameters.
     */
    ServiceResult<Collection<DidDocument>> queryDocuments(QuerySpec spec);

    default String notFoundMessage(String did) {
        return "A DID document with ID '%s' does not exist.".formatted(did);
    }

    default String noPublisherFoundMessage(String did) {
        return "No publisher was found for DID '%s'".formatted(did);
    }

    DidResource findById(String did);

    ServiceResult<Void> addService(String did, Service service);

    ServiceResult<Void> replaceService(String did, Service service);

    ServiceResult<Void> removeService(String did, String serviceId);
}
