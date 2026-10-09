/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.participantcontext;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Fails the creation of a participant context when one of the steps that provision it fails. It is thrown within the
 * transaction that creates the participant context, so that the transaction is rolled back, and carries the failure out of it.
 */
class ProvisioningException extends EdcException {
    private final ServiceResult<?> failure;

    ProvisioningException(ServiceResult<?> failure) {
        super(failure.getFailureDetail());
        this.failure = failure;
    }

    <T> ServiceResult<T> failure() {
        return failure.mapFailure();
    }
}
