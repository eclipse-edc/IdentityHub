/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer.spi;

import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.result.ServiceResult;

public interface DcpIssuerMetadataService {

    /**
     * Returns the metadata of the issuer.
     *
     * @param participantContext the participant context
     * @return the metadata of the issuer
     */
    ServiceResult<IssuerMetadata> getIssuerMetadata(ParticipantContext participantContext);
}
