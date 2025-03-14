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

package org.eclipse.edc.identityhub.protocols.dcp.spi.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;

/**
 * Represents a DCP profile identified by an alias and identifies a combination of a credential format and a status list type.
 */
public record DcpProfile(String name, CredentialFormat format, String statusListType) {
}
