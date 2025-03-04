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

package org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;

import java.util.List;
import java.util.Map;

/**
 * DTO for an {@link IssuanceProcess}.
 */
public record IssuanceProcessDto(String id,
                                 String holderId,
                                 String participantContextId,
                                 String holderPid,
                                 Map<String, Object> claims,
                                 List<String> credentialDefinitions,
                                 Map<String, CredentialFormat> credentialFormats,
                                 String state,
                                 long createdAt,
                                 long updatedAt) {


    public static IssuanceProcessDto fromIssuanceProcess(IssuanceProcess issuanceProcess) {
        return new IssuanceProcessDto(issuanceProcess.getId(),
                issuanceProcess.getHolderId(),
                issuanceProcess.getParticipantContextId(),
                issuanceProcess.getHolderPid(),
                issuanceProcess.getClaims(),
                issuanceProcess.getCredentialDefinitions(),
                issuanceProcess.getCredentialFormats(),
                issuanceProcess.stateAsString(),
                issuanceProcess.getCreatedAt(),
                issuanceProcess.getUpdatedAt());
    }
}
