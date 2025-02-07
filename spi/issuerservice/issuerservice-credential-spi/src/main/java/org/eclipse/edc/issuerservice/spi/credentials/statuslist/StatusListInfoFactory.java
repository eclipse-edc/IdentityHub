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

package org.eclipse.edc.issuerservice.spi.credentials.statuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Creates {@link StatusListInfo} objects depending on the {@link CredentialStatus} object of the holder credential. This
 * is independent of the status purpose, but a separate {@link StatusListInfo} should be created for each status purpose.
 */
public interface StatusListInfoFactory {
    /**
     * Creates a {@link StatusListInfo} object based on the credential status of the holder credential. Holder credential
     * may have multiple status objects, and one {@link StatusListInfo} must be created each.
     *
     * @param credentialStatus The credential status
     */
    ServiceResult<StatusListInfo> create(CredentialStatus credentialStatus);
}
