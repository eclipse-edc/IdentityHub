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
     * "Links" (=enables) an {@link AttestationDefinition} for a given {@code Participant}. When a participant makes an issuance request,
     * they can only request claims (attestation data) from AttestationDefinitions that are enabled for them.
     *
     * @param attestationId The ID of the {@link AttestationDefinition}
     * @param participantId The ID of the {@code Participant}
     * @return success with {@link Boolean#TRUE} if the link was created successfully, or {@link Boolean#FALSE} if the link already existed, a failure otherwise.
     */
    ServiceResult<Boolean> linkAttestation(String attestationId, String participantId);

    /**
     * "Unlinks" (=disables) an {@link AttestationDefinition} for a given {@code Participant}. When a participant makes an issuance request,
     * they can only request claims (attestation data) from AttestationDefinitions that are enabled for them. Removing the link
     * disables a certain {@link AttestationDefinition}
     *
     * @param attestationId The ID of the {@link AttestationDefinition}
     * @param participantId The ID of the {@code Participant}
     * @return success with {@link Boolean#TRUE} if the link was deleted successfully, {@link Boolean#FALSE} if no link existed, a failure otherwise.
     */
    ServiceResult<Boolean> unlinkAttestation(String attestationId, String participantId);

    /**
     * Gets all attestations for a given participant.
     *
     * @param participantId the ID of the participant
     * @return A (potentially empty) list of {@link AttestationDefinition} objects, or an error if the participant was not found.
     */
    ServiceResult<Collection<AttestationDefinition>> getAttestationsForParticipant(String participantId);

    /**
     * Queries all {@link AttestationDefinition} objects according to a given query.
     *
     * @param querySpec the query
     * @return A (potentially empty) list of {@link AttestationDefinition} objects, or an error if the query was malformed.
     */
    ServiceResult<Collection<AttestationDefinition>> queryAttestations(QuerySpec querySpec);
}
