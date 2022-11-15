/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.spi;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * IdentityHubStore used to store data in an Identity Hub.
 */
public interface IdentityHubStore {

    /**
     * List all {@link IdentityHubRecord}.
     *
     * @return Stream of store items.
     */
    @NotNull Stream<IdentityHubRecord> getAll();

    /**
     * Put a new record in the store.
     *
     * @param record Record to be put in the store.
     * @throws org.eclipse.edc.spi.persistence.EdcPersistenceException if a record with the exact same id is already present in the store.
     */
    void add(IdentityHubRecord record);
}
