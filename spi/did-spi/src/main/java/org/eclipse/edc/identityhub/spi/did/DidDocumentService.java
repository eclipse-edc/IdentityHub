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

package org.eclipse.edc.identityhub.spi.did;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

/**
 * The {@code DidDocumentService} gives access to a {@link DidDocument} that are held in storage.
 */
public interface DidDocumentService {

    /**
     * Stores a DID document in persistent storage. * * @param document the {@link DidDocument} to store
     *
     * @return a {@link ServiceResult} to indicate success or failure.
     */
    ServiceResult<Void> store(DidDocument document, String participantId);

    /**
     * Deletes a DID document if found. * * @param did The ID of the DID document to delete.
     *
     * @return A {@link ServiceResult} indicating success or failure.
     */
    ServiceResult<Void> deleteById(String did);

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

    /**
     * Adds a service endpoint entry to a did document.
     *
     * @param did     The DID of the document to which the entry should be added.
     * @param service The service endpoint to add.
     * @return success if added, a failure otherwise, e.g. because that same service already exists.
     */
    ServiceResult<Void> addService(String did, Service service);

    /**
     * Replaces a service endpoint entry in a did document.
     *
     * @param did     The DID of the document in which the entry should be replaced.
     * @param service The new service endpoint .
     * @return success if replaced, a failure otherwise, e.g. because a service with that ID does not exist exists.
     */
    ServiceResult<Void> replaceService(String did, Service service);

    /**
     * Removes a service endpoint entry from a did document.
     *
     * @param did       The DID of the document from which the entry should be removed.
     * @param serviceId The service endpoint to remove.
     * @return success if removed, a failure otherwise, e.g. because a service with that ID does not exist exists.
     */
    ServiceResult<Void> removeService(String did, String serviceId);
}
