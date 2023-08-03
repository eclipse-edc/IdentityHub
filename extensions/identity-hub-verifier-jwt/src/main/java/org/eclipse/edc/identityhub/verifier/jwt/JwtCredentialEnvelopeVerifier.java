/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.Verifiable;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifier;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Implementation of a Verifiable Credentials verifier working with JWT format
 *
 * @see <a href="https://www.w3.org/TR/vc-data-model/#example-usage-of-the-id-property">vc-data-model</a>
 */
public class JwtCredentialEnvelopeVerifier extends JwtEnvelopeVerifier implements CredentialEnvelopeVerifier<JwtCredentialEnvelope> {

    private final ObjectMapper mapper;

    public JwtCredentialEnvelopeVerifier(JwtCredentialsVerifier jwtCredentialsVerifier, ObjectMapper mapper) {
        super(jwtCredentialsVerifier);
        this.mapper = mapper;
    }

    @Override
    public Result<List<Credential>> verify(JwtCredentialEnvelope vc, DidDocument didDocument) {
        var jwt = vc.getJwt();
        var result = verifyJwtClaims(jwt, didDocument);
        if (result.failed()) {
            return Result.failure(result.getFailureMessages());
        }
        var signatureResult = verifySignature(jwt);

        if (signatureResult.failed()) {
            return Result.failure(signatureResult.getFailureMessages());
        }
        var verifiableCredentialResult = vc.toVerifiableCredentials(mapper);
        if (verifiableCredentialResult.failed()) {
            return Result.failure(verifiableCredentialResult.getFailureMessages());
        }

        return Result.success(verifiableCredentialResult.getContent().stream().map(Verifiable::getItem).toList());
    }
}
