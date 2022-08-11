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

package org.eclipse.dataspaceconnector.identityhub.verifier;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Failure;

import java.util.List;

/**
 * A generic result type containing the result of processing plus the failures happening during the processing of the
 * result.
 * For example, when processing a List of objects, the processing can be successful for some objects, and unsuccessful
 * for others.
 * Using this class as a return type would let the client decide how to act about the failures.
 *
 * @param <T> Result type
 */
class AggregatedResult<T> extends AbstractResult<T, Failure> {
    AggregatedResult(T successfulResult, List<String> failureMessage) {
        super(successfulResult, failureMessage.isEmpty() ? null : new Failure(failureMessage));
    }
}