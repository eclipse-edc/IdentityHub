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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.spi;

import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Service interface for the DCP Issuer service.
 */
public interface DcpIssuerService {

    /**
     * Initiates the issuance of credentials.
     *
     * @param message the credential request message
     * @param context the DCP request context
     * @return the result of the issuance initiation
     */
    ServiceResult<CredentialRequestMessage.Response> initiateCredentialsIssuance(String participantContextId, CredentialRequestMessage message, DcpRequestContext context);
}
