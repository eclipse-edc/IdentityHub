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

package org.eclipse.edc.identityhub.protocols.dcp.spi;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

/**
 * Validates Self-Issued ID tokens sent from an issuer upon receiving a {@code CredentialMessage}
 */

@ExtensionPoint
public interface DcpIssuerTokenVerifier {

    Result<ClaimToken> verify(ParticipantContext participantContext, TokenRepresentation tokenRepresentation);

    default Result<ClaimToken> verify(ParticipantContext participantContext, String token) {
        return verify(participantContext, TokenRepresentation.Builder.newInstance().token(token).build());
    }
}
