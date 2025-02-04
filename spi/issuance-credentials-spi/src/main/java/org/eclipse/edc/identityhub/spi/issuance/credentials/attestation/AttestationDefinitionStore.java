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

package org.eclipse.edc.identityhub.spi.issuance.credentials.attestation;

import org.eclipse.edc.identityhub.spi.issuance.credentials.model.AttestationDefinition;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

/**
 * Persists attestation definitions.
 */
public interface AttestationDefinitionStore {

    /**
     * Returns a definition for the id or null if not found.
     */
    @Nullable
    AttestationDefinition resolveDefinition(String id);

    /**
     * Persists a new attestation definition.
     */
    StoreResult<Void> create(AttestationDefinition credentialResource);

    /**
     * Updates an existing attestation definition.
     */
    StoreResult<Void> update(AttestationDefinition credentialResource);

    /**
     * Removes an attestation from persistent storage.
     */
    StoreResult<Void> delete(String id);

}
