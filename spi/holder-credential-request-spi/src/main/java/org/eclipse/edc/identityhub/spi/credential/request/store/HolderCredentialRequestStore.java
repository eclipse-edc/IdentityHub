/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credential.request.store;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * Stores {@link HolderCredentialRequest} objects
 */
public interface HolderCredentialRequestStore extends StateEntityStore<HolderCredentialRequest> {

    Collection<HolderCredentialRequest> query(QuerySpec query);

    /**
     * Deletes a request. A request that is leased by someone else is not deleted.
     *
     * @param id the ID of the request
     * @return success, or a failure if the request does not exist or is leased
     */
    StoreResult<Void> deleteById(String id);
}
