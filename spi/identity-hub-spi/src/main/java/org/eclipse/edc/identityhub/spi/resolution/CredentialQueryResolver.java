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

package org.eclipse.edc.identityhub.spi.resolution;

import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Resolves a list of {@link VerifiableCredentialContainer} objects based on an incoming {@link PresentationQuery} and a list of scope strings.
 */
public interface CredentialQueryResolver {

    /**
     * Query method for fetching credentials. If this method returns a successful result, it will contain a list of {@link VerifiableCredentialContainer}.
     * If a failure is returned, that means that the given query does not match the given issuer scopes, which would be equivalent to an unauthorized access (c.f. HTTP 403 error).
     * The Result could also contain information about any errors or issues the occurred during the query execution.
     *
     * @param query        The representation of the query to be executed.
     * @param issuerScopes The list of issuer scopes to be considered during the query processing.
     */
    Result<List<VerifiableCredentialContainer>> query(PresentationQuery query, List<String> issuerScopes);
}