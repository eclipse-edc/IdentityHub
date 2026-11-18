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

package org.eclipse.edc.issuerservice.spi.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;
import java.util.Map;

public interface HolderService {

    ServiceResult<Void> createHolder(Holder holder);

    ServiceResult<Void> deleteHolder(String participantContextId, String holderId);

    ServiceResult<Void> updateHolder(Holder holder);

    /**
     * Updates the properties of an existing holder, leaving everything else as it is. The holder is looked up by its participant
     * context ID and holder ID. The given DID and name must be equal to those of the stored holder, they cannot be changed.
     *
     * @param participantContextId the participant context that owns the holder
     * @param holderId             the ID of the holder
     * @param did                  the DID of the holder, must match the stored holder
     * @param name                 the name of the holder, must match the stored holder
     * @param properties           entries to add to the holder's properties. The value of an existing key is replaced.
     * @param removedProperties    keys to remove from the holder's properties. Keys that the holder does not have are ignored.
     * @return the holder as it was stored, a not-found failure if the holder does not exist, or a bad-request failure if the
     *         DID or the name do not match the stored holder.
     */
    ServiceResult<Holder> patchHolder(String participantContextId, String holderId, String did, String name, Map<String, Object> properties, Collection<String> removedProperties);

    ServiceResult<Collection<Holder>> queryHolders(QuerySpec querySpec);

    ServiceResult<Holder> findById(String participantContextId, String holderId);
}
