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

import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Manages status list credentials on the issuer side. Whenever an issuer issues a credential, it should also add a
 * <a href="https://www.w3.org/TR/vc-data-model-2.0/#status">{@code credentialStatus}</a> object to the holder credential.
 * <p>
 * Status list managers should consider the following situations:
 * <ul>
 *   <li>No status list credential exists yet -> create a new one</li>
 *   <li>Status list credential is saturated (= all bits in the bitstring are occupied)</li>
 *   <li>Creating new status list credentials should also publish them and retrieve the public URL</li>
 * </ul>
 */
public interface StatusListManager {
    /**
     * the current status list index. needed to detect overflow or "fullness"s
     */
    String CURRENT_INDEX = "currentIndex";
    /**
     * the public URL where the status list credential can be obtained
     */
    String PUBLIC_URL = "publicUrl";
    /**
     * marks the "active" credential, i.e. the ones where new holder credentials get added
     */
    String IS_ACTIVE = "isActive";

    /**
     * Obtains the currently active status list credential for a particular participant context id (=tenant). If the current
     * status list credential is saturated, a new one is created and published transparently and then returned
     *
     * @param participantContextId The Issuer participant context id
     * @return the currently active, non-saturated status list credential entry
     */
    ServiceResult<StatusListCredentialEntry> getActiveCredential(String participantContextId);

    /**
     * Increments the current status list index of the active status list credential. Note that this method does not check whether
     * the increment operation saturates the credential.
     *
     * @param entry the currently active status list credential
     * @return a service result to indicate the success of the operation
     */
    ServiceResult<Void> incrementIndex(StatusListCredentialEntry entry);
}
