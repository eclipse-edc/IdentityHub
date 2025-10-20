/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

public enum CredentialUsage {
    /**
     * Credentials that are used by a holder for DCP requests.
     */
    Holder,
    /**
     * StatusList credentials. Not to be used for DCP interactions.
     */
    StatusList,
    /**
     * Credentials that an issuer stores in order to track what has been issued to whom. Typically, those do not carry the signed credential
     */
    IssuanceTracking
}
