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
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.text.ParseException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Obtains and verifies credentials associated with a DID.
 * The DID document contains an IdentityHub service, the IdentityHubCredentialsVerifier gets credentials from the
 * IdentityHub instance and verifies the credentials.
 */
public class IdentityHubCredentialsVerifier implements CredentialsVerifier {

    private final IdentityHubClient identityHubClient;
    private final Monitor monitor;
    private final DidPublicKeyResolver didPublicKeyResolver;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient IdentityHubClient.
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, DidPublicKeyResolver didPublicKeyResolver) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.didPublicKeyResolver = didPublicKeyResolver;
    }

    /**
     * Get credentials from the IdentityHub of participant, verifies the credentials, and returns the successfully verified credentials.
     * To be successfully verified, credentials needs to be signed by the specified issuer in the JWT.
     *
     * @param didDocument of a participant. The Did Document should contain an IdentityHub service.
     * @return VerifiableCredentials.
     */
    @Override
    public Result<Map<String, Object>> verifyCredentials(DidDocument didDocument) {
        var hubBaseUrl = getIdentityHubBaseUrl(didDocument);
        if (hubBaseUrl.failed()) return Result.failure(hubBaseUrl.getFailureMessages());

        var jwts = identityHubClient.getVerifiableCredentials(hubBaseUrl.getContent());
        if (jwts.failed()) {
            return Result.failure(jwts.getFailureMessages());
        }
        var verifiedClaims = jwts.getContent()
                .stream()
                .filter(this::verify);
        var claims = verifiedClaims.map(this::getClaims)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Result.success(claims);
    }

    private Result<Map.Entry<String, Object>> getClaims(SignedJWT jwt) {
        try {
            var jwtClaims = jwt.getJWTClaimsSet().getClaims();
            var payload = (HashMap<String, Object>) jwt.getPayload().toJSONObject();
            var vc = (HashMap<String, Object>) payload.get("vc");
            var credentialId = vc.get("id").toString();
            payload.putAll(jwtClaims);

            return Result.success(new AbstractMap.SimpleEntry<>(credentialId, payload));
        } catch (ParseException | RuntimeException e) {
            return Result.failure(e.getMessage());
        }
    }

    private boolean verify(SignedJWT jwt) {
        var issuer = getIssuer(jwt);
        if (issuer.failed()) {
            return false;
        }
        var issuerPublicKey = didPublicKeyResolver.resolvePublicKey(issuer.getContent());
        if (issuerPublicKey.failed()) {
            return false;
        }
        var verificationResult = VerifiableCredentialFactory.verify(jwt, issuerPublicKey.getContent(), "identity-hub");
        return verificationResult.succeeded();
    }

    private Result<String> getIssuer(SignedJWT jwt) {
        try {
            return Result.success(jwt.getJWTClaimsSet().getIssuer());
        } catch (ParseException e) {
            monitor.info("Error parsing issuer from JWT", e);
            return Result.failure(e.getMessage());
        }
    }

    private Result<String> getIdentityHubBaseUrl(DidDocument didDocument) {
        var hubBaseUrl = didDocument
                .getService()
                .stream()
                .filter(s -> s.getType().equals("IdentityHub"))
                .findFirst();

        if (hubBaseUrl.isEmpty()) return Result.failure("Failed getting identityHub URL");
        else return Result.success(hubBaseUrl.get().getServiceEndpoint());
    }
}
