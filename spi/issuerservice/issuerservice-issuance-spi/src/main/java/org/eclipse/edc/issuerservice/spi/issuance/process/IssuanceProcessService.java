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

package org.eclipse.edc.issuerservice.spi.issuance.process;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service for managing {@link IssuanceProcess}.
 */
public interface IssuanceProcessService {

    /**
     * Find a {@link IssuanceProcess} by its ID.
     *
     * @param id The ID
     * @return {@link IssuanceProcess} or null if not found
     */
    IssuanceProcess findById(String id);

    /**
     * Search {@link IssuanceProcess}.
     *
     * @param query The query
     * @return The collection of {@link IssuanceProcess} that match the query
     */
    ServiceResult<List<IssuanceProcess>> search(QuerySpec query);

}
