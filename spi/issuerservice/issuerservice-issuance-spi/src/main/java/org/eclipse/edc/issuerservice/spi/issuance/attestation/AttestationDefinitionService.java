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

package org.eclipse.edc.issuerservice.spi.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public interface AttestationDefinitionService {

    /**
     * Creates an {@link AttestationDefinition} in persistent storage
     *
     * @param attestationDefinition The attestation definition
     */
    ServiceResult<Void> createAttestation(AttestationDefinition attestationDefinition);

    /**
     * Deletes an {@link AttestationDefinition} from persistent storage
     *
     * @param attestationId the ID of the attestation definition
     */
    ServiceResult<Void> deleteAttestation(String attestationId);

    /**
     * Gets an {@link AttestationDefinition} by id.
     *
     * @param attestationId the ID of the attestation
     * @return A {@link AttestationDefinition} if found, or an error if it was not found.
     */
    ServiceResult<AttestationDefinition> getAttestationById(String attestationId);

    /**
     * Queries all {@link AttestationDefinition} objects according to a given query.
     *
     * @param querySpec the query
     * @return A (potentially empty) list of {@link AttestationDefinition} objects, or an error if the query was malformed.
     */
    ServiceResult<Collection<AttestationDefinition>> queryAttestations(QuerySpec querySpec);
}
