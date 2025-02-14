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

package org.eclipse.edc.issuerservice.spi.issuance;

import java.util.Map;

/**
 * A context containing data used during the issuance process.
 */
public interface IssuanceContext {

    /**
     * Returns a claim or null if not found.
     */
    @SuppressWarnings("unchecked")
    default <T> T getClaim(String claim) {
        return (T) getClaims().get(claim);
    }

    /**
     * Returns all claims.
     */
    Map<String, Object> getClaims();
}
