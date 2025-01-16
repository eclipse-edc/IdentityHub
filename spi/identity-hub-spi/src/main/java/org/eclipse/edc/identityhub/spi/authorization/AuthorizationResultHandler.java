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

package org.eclipse.edc.identityhub.spi.authorization;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ServiceResultHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Extension of the {@link ServiceResultHandler} which also can handle {@link ServiceFailure.Reason#UNAUTHORIZED} failures. All other
 * failures are delegated back to the {@link ServiceResultHandler}.
 */
public class AuthorizationResultHandler {
    public static Function<ServiceFailure, EdcException> exceptionMapper(@NotNull Class<?> clazz, String id) {
        return failure -> {
            if (failure.getReason() == ServiceFailure.Reason.UNAUTHORIZED) {
                return new NotAuthorizedException(failure.getFailureDetail());
            }
            return ServiceResultHandler.exceptionMapper(clazz, id).apply(failure);
        };
    }

    public static Function<ServiceFailure, EdcException> exceptionMapper(@NotNull Class<?> clazz) {
        return failure -> {
            if (failure.getReason() == ServiceFailure.Reason.UNAUTHORIZED) {
                return new NotAuthorizedException(failure.getFailureDetail());
            }
            return ServiceResultHandler.exceptionMapper(clazz).apply(failure);
        };
    }
}
