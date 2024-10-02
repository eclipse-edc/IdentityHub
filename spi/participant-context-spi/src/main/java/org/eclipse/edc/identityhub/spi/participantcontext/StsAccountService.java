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

package org.eclipse.edc.identityhub.spi.participantcontext;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.result.ServiceResult;

public interface StsAccountService {
    ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias);

    ServiceResult<Void> deleteAccount(String participantId);

    ServiceResult<Void> updateAccount(StsAccount modificationFunction);

    ServiceResult<StsAccount> findById(String id);
}
