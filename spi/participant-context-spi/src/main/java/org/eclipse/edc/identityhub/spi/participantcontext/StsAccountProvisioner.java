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

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * The STS Account provisioner listens for certain events in an IdentityHub, for example {@link org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated}
 * and performs related actions such as creating, updating or deleting STS Accounts.
 */
public interface StsAccountProvisioner {
    String CLIENT_SECRET_PROPERTY = "clientSecret";

    ServiceResult<AccountInfo> create(ParticipantManifest manifest);
}
