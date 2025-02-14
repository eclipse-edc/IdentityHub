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

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcess;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service for managing {@link IssuerCredentialIssuanceProcess}.
 */
public interface IssuerCredentialIssuanceProcessService {

    /**
     * Find a {@link IssuerCredentialIssuanceProcess} by its ID.
     *
     * @param id The ID
     * @return {@link IssuerCredentialIssuanceProcess} or null if not found
     */
    IssuerCredentialIssuanceProcess findById(String id);

    /**
     * Search {@link IssuerCredentialIssuanceProcess}.
     *
     * @param query The query
     * @return The collection of {@link IssuerCredentialIssuanceProcess} that match the query
     */
    ServiceResult<List<IssuerCredentialIssuanceProcess>> search(QuerySpec query);

}
