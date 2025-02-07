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

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;

/**
 * A StatusListInfo is a proxy object that transparently allows getting and setting the status flag of a status list credential.
 * This does not specify the status purpose, it merely gets and sets status bits on the status list credential.
 * <p>
 * So, if there are two status list credentials, one for "revocation", one for "suspension", then there would be two {@link StatusListInfo} instances.
 * <p>
 * {@link StatusListInfo} objects are created by a {@link StatusListInfoFactory}.
 */
public interface StatusListInfo {
    Result<String> getStatus();

    Result<Void> setStatus(boolean status);

    VerifiableCredentialResource statusListCredential();
}
