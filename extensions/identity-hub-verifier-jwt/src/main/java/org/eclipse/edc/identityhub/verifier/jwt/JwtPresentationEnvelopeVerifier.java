/*
 *  Copyright (c) 2023 GAIA-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       GAIA-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.credentials.jwt.JwtPresentationEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.Verifiable;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifier;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Implementation of a Verifiable Presentation verifier working with JWT format
 *
 * @see <a href="https://www.w3.org/TR/vc-data-model/#json-web-token">W3C specification</a>
 */
public class JwtPresentationEnvelopeVerifier extends JwtEnvelopeVerifier implements CredentialEnvelopeVerifier<JwtPresentationEnvelope> {

    private final ObjectMapper mapper;

    public JwtPresentationEnvelopeVerifier(JwtCredentialsVerifier jwtCredentialsVerifier, ObjectMapper mapper) {
        super(jwtCredentialsVerifier);
        this.mapper = mapper;
    }

    @Override
    public Result<List<Credential>> verify(JwtPresentationEnvelope vp, DidDocument didDocument) {
        var jwt = vp.getJwt();
        var result = verifyJwtClaims(jwt, didDocument);
        if (result.failed()) {
            return Result.failure(result.getFailureMessages());
        }
        var signatureResult = verifySignature(jwt);

        if (signatureResult.failed()) {
            return Result.failure(signatureResult.getFailureMessages());
        }
        var verifiableCredentialResult = vp.toVerifiableCredentials(mapper);
        if (verifiableCredentialResult.failed()) {
            return Result.failure(verifiableCredentialResult.getFailureMessages());
        }

        var jwtVerifyResult = Result.success(verifiableCredentialResult.getContent().stream().map(Verifiable::getItem).toList());

        if (jwtVerifyResult.failed()) {
            return Result.failure(jwtVerifyResult.getFailureMessages());
        }

        var jwtVerifiableCredentials = vp.toJwtVerifiableCredentials(mapper);
        if (jwtVerifiableCredentials.failed()) {
            return Result.failure(jwtVerifiableCredentials.getFailureMessages());
        }
        var vcResult = jwtVerifiableCredentials.getContent().stream().map(jwtVc -> {
            var verifyJwtClaimsResult = verifyJwtClaims(jwtVc, didDocument);
            if (verifyJwtClaimsResult.failed()) {
                return Result.failure(verifyJwtClaimsResult.getFailureMessages());
            }

            return verifySignature(jwtVc);
        }).filter(Result::failed).findFirst().orElse(Result.success());

        if (vcResult.failed()) {
            return Result.failure(vcResult.getFailureMessages());
        }

        return jwtVerifyResult;
    }

}
