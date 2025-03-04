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

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.Map;

/**
 * Context for a DCP request. Contains the {@link Holder} and a set of claims
 * that might come from a DCP presentation request.
 */
public record DcpRequestContext(Holder holder, Map<String, ClaimToken> claims) {

}
