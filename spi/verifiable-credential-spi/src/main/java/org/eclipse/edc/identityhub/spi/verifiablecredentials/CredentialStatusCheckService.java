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

package org.eclipse.edc.identityhub.spi.verifiablecredentials;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;

/**
 * Checks if a {@link VerifiableCredentialResource} is revoked, expired, not-yet-valid or suspended. Once a credential is {@link VcStatus#EXPIRED},
 * it can never transition to another status.
 * {@link VcStatus#EXPIRED} and {@link VcStatus#REVOKED} are non-reversible (terminal) states. Once reversible
 * states ({@link VcStatus#NOT_YET_VALID} and {@link VcStatus#SUSPENDED}) are cleared, the default state {@link VcStatus#ISSUED} is assumed.
 */
@FunctionalInterface
public interface CredentialStatusCheckService {
    /**
     * Checks the current status of a {@link VerifiableCredentialResource}. Note that the status returned by this method
     * is not an indicator of a state transition, so client code should check for a change.
     *
     * @param resource The resource to check. {@link VerifiableCredentialResource#getVerifiableCredential()} cannot be null.
     * @return A successful result with the new status, or a failure if a check (e.g. a remote call) failed.
     */
    Result<VcStatus> checkStatus(VerifiableCredentialResource resource);
}
