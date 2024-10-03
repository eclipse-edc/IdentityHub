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

package org.eclipse.edc.identityhub.common.provisioner;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Default implementation for an STS Account service, that doesn't do anything.
 */
class NoopAccountService implements StsAccountService {
    @Override
    public ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias) {
        return ServiceResult.success();
    }

    @Override
    public ServiceResult<Void> deleteAccount(String participantId) {
        return ServiceResult.success();
    }

    @Override
    public ServiceResult<Void> updateAccount(StsAccount updatedAccount) {
        return ServiceResult.success();
    }

    @Override
    public ServiceResult<StsAccount> findById(String id) {
        return ServiceResult.success(null);
    }
}
