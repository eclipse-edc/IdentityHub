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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.AbstractResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryFailure.Reason.INVALID_SCOPE;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryFailure.Reason.STORAGE_FAILURE;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryFailure.Reason.UNAUTHORIZED_SCOPE;

/**
 * Represents a query executed by the {@link CredentialQueryResolver}
 */
public class QueryResult extends AbstractResult<Stream<VerifiableCredentialContainer>, QueryFailure, QueryResult> {
    protected QueryResult(Stream<VerifiableCredentialContainer> content, QueryFailure failure) {
        super(content, failure);
    }

    /**
     * The query failed because no scope string was found
     */
    public static QueryResult noScopeFound(String message) {
        return new QueryResult(null, new QueryFailure(List.of(message), INVALID_SCOPE));
    }

    /**
     * The query failed because the credential storage reported an error
     */
    public static QueryResult storageFailure(List<String> failureMessages) {
        return new QueryResult(null, new QueryFailure(failureMessages, STORAGE_FAILURE));
    }

    /**
     * The query failed, because the scope string was not valid (format, allowed values, etc.)
     */
    public static QueryResult invalidScope(List<String> failureMessages) {
        return new QueryResult(null, new QueryFailure(failureMessages, INVALID_SCOPE));
    }

    /**
     * The query failed because the query is unauthorized, e.g. by insufficiently broad scopes
     */
    public static QueryResult unauthorized(String failureMessage) {
        return new QueryResult(null, new QueryFailure(List.of(failureMessage), UNAUTHORIZED_SCOPE));
    }

    /**
     * Query successful. List of credentials is in the content.
     */
    public static QueryResult success(Stream<VerifiableCredentialContainer> credentials) {
        return new QueryResult(credentials, null);
    }

    public QueryFailure.Reason reason() {
        return getFailure().getReason();
    }

    @Override
    protected <R1 extends AbstractResult<C1, QueryFailure, R1>, C1> @NotNull R1 newInstance(@Nullable C1 content, @Nullable QueryFailure failure) {
        if (content instanceof Stream) {
            return (R1) new QueryResult((Stream) content, failure);
        }
        return (R1) new QueryResult(null, failure);
    }

}
