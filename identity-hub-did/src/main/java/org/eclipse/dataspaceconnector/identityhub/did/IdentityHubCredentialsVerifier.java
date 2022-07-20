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

package org.eclipse.dataspaceconnector.identityhub.did;

import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Obtains and verifies credentials associated with a DID.
 * The DID document contains an IdentityHub service, the IdentityHubCredentialsVerifier gets credentials from the
 * IdentityHub instance and verifies the credentials.
 */
public class IdentityHubCredentialsVerifier implements CredentialsVerifier {

    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    private static final String CREDENTIALS_ID_KEY = "id";
    private static final String IDENTITY_HUB_SERVICE_TYPE = "IdentityHub";
    private final IdentityHubClient identityHubClient;
    private final Monitor monitor;
    private final SignatureVerifier signatureVerifier;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient IdentityHubClient.
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, SignatureVerifier signatureVerifier) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.signatureVerifier = signatureVerifier;
    }

    /**
     * Get credentials from the IdentityHub of participant, verifies the credentials, and returns the successfully verified credentials.
     * To be successfully verified, credentials needs to be signed by the specified issuer in the JWT.
     *
     * @param didDocument of a participant. The Did Document should contain an IdentityHub service.
     * @return VerifiableCredentials.
     */
    @Override
    public Result<Map<String, Object>> getVerifiedCredentials(DidDocument didDocument) {
        var hubBaseUrl = getIdentityHubBaseUrl(didDocument);
        if (hubBaseUrl.failed()) return Result.failure(hubBaseUrl.getFailureMessages());

        var jwts = identityHubClient.getVerifiableCredentials(hubBaseUrl.getContent());
        if (jwts.failed()) {
            return Result.failure(jwts.getFailureMessages());
        }
        var verifiedClaims = jwts.getContent()
                .stream()
                .filter(signatureVerifier::isSignedByIssuer);
        var claims = verifiedClaims.map(this::extractCredential)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Result.success(claims);
    }

    private Result<Map.Entry<String, Object>> extractCredential(SignedJWT jwt) {
        try {
            var payload = jwt.getPayload().toJSONObject();
            var credentialId = extractVcId(payload);
            if (credentialId.failed()) return Result.failure(credentialId.getFailureMessages());

            return Result.success(new AbstractMap.SimpleEntry<>(credentialId.getContent(), payload));
        } catch (RuntimeException e) {
            return Result.failure(e.getMessage() == null ? e.getClass().toString() : e.getMessage());
        }
    }

    private Result<String> extractVcId(JSONObject jsonPayload) {
        var payload = (Map<String, Object>) jsonPayload;

        if (!payload.containsKey(VERIFIABLE_CREDENTIALS_KEY)) {
            return Result.failure(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
        }
        var vc = (Map<String, Object>) payload.get(VERIFIABLE_CREDENTIALS_KEY);
        var vcId = vc.get(CREDENTIALS_ID_KEY);
        return vcId == null ? Result.failure("vc id not found") : Result.success(vcId.toString());
    }

    private Result<String> getIdentityHubBaseUrl(DidDocument didDocument) {
        var hubBaseUrl = didDocument
                .getService()
                .stream()
                .filter(s -> s.getType().equals(IDENTITY_HUB_SERVICE_TYPE))
                .findFirst();

        if (hubBaseUrl.isEmpty()) return Result.failure("Failed getting identityHub URL");
        else return Result.success(hubBaseUrl.get().getServiceEndpoint());
    }
}
