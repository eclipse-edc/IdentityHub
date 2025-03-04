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
     * "Links" (=enables) an {@link AttestationDefinition} for a given {@code Holder}. When a holder makes an issuance request,
     * they can only request claims (attestation data) from AttestationDefinitions that are enabled for them.
     *
     * @param attestationId The ID of the {@link AttestationDefinition}
     * @param holderId      The ID of the {@code Holder}
     * @return success with {@link Boolean#TRUE} if the link was created successfully, or {@link Boolean#FALSE} if the link already existed, a failure otherwise.
     */
    ServiceResult<Boolean> linkAttestation(String attestationId, String holderId);

    /**
     * "Unlinks" (=disables) an {@link AttestationDefinition} for a given {@code Holder}. When a holder makes an issuance request,
     * they can only request claims (attestation data) from AttestationDefinitions that are enabled for them. Removing the link
     * disables a certain {@link AttestationDefinition}
     *
     * @param attestationId The ID of the {@link AttestationDefinition}
     * @param holderId      The ID of the {@code Holder}
     * @return success with {@link Boolean#TRUE} if the link was deleted successfully, {@link Boolean#FALSE} if no link existed, a failure otherwise.
     */
    ServiceResult<Boolean> unlinkAttestation(String attestationId, String holderId);

    /**
     * Gets all attestations for a given holder.
     *
     * @param holderId the ID of the holder
     * @return A (potentially empty) list of {@link AttestationDefinition} objects, or an error if the holder was not found.
     */
    ServiceResult<Collection<AttestationDefinition>> getAttestationsForHolder(String holderId);

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
