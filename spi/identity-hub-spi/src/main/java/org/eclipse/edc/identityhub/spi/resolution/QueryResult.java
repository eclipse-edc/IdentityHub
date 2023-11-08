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

import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.AbstractResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.spi.resolution.QueryFailure.Reason.INVALID_SCOPE;
import static org.eclipse.edc.identityhub.spi.resolution.QueryFailure.Reason.OTHER;
import static org.eclipse.edc.identityhub.spi.resolution.QueryFailure.Reason.STORAGE_FAILURE;
import static org.eclipse.edc.identityhub.spi.resolution.QueryFailure.Reason.UNAUTHORIZED_SCOPE;


public class QueryResult extends AbstractResult<Stream<VerifiableCredentialContainer>, QueryFailure, QueryResult> {
    protected QueryResult(Stream<VerifiableCredentialContainer> content, QueryFailure failure) {
        super(content, failure);
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

    public static QueryResult other(String... message) {
        return new QueryResult(null, new QueryFailure(Arrays.asList(message), OTHER));
    }

    public static QueryResult noScopeFound(String message) {
        return new QueryResult(null, new QueryFailure(List.of(message), INVALID_SCOPE));
    }

    public static QueryResult storageFailure(List<String> failureMessages) {
        return new QueryResult(null, new QueryFailure(failureMessages, STORAGE_FAILURE));
    }

    public static QueryResult invalidScope(List<String> failureMessages) {
        return new QueryResult(null, new QueryFailure(failureMessages, INVALID_SCOPE));
    }

    public static QueryResult unauthorized(String failureMessage) {
        return new QueryResult(null, new QueryFailure(List.of(failureMessage), UNAUTHORIZED_SCOPE));
    }

    public static QueryResult success(Stream<VerifiableCredentialContainer> credentials) {
        return new QueryResult(credentials, null);
    }

}
