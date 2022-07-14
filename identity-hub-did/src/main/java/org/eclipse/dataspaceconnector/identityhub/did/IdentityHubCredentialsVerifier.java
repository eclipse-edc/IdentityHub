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

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class IdentityHubCredentialsVerifier implements CredentialsVerifier {

    private final IdentityHubClient identityHubClient;
    private final Monitor monitor;
    private final DidPublicKeyResolver didPublicKeyResolver;
    private final ObjectMapper objectMapper;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient IdentityHubClient.
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, DidPublicKeyResolver didPublicKeyResolver, ObjectMapper objectMapper) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.didPublicKeyResolver = didPublicKeyResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Map<String, Object>> getVerifiedClaims(DidDocument didDocument) {
        var hubBaseUrl = getIdentityHubBaseUrl(didDocument);
        if (hubBaseUrl.failed()) return Result.failure(hubBaseUrl.getFailureMessages());

        var jwts = identityHubClient.getVerifiableCredentials(hubBaseUrl.getContent());
        if (jwts.failed()) return Result.failure(jwts.getFailureMessages());
        var claims = jwts.getContent()
                .stream()
                .filter(this::verify)
                .map(this::getClaims)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Result.success(claims);
    }

    private Result<Map.Entry<String, Object>> getClaims(SignedJWT jwt) {
        try {
            Map<String, Object> jwtClaims = jwt.getJWTClaimsSet().getClaims();
            var vc = (HashMap<String, Object>) jwt.getPayload().toJSONObject().get("vc");
            vc.putAll(jwtClaims);
            var credentialId = vc.get("id").toString();
            return Result.success(new AbstractMap.SimpleEntry<String, Object>(credentialId, vc));
        } catch (ParseException | RuntimeException e) {
            return Result.failure(e.getMessage());
        }
    }

    private boolean verify(SignedJWT jwt) {
        var issuer = getIssuer(jwt);
        if (issuer.failed()) return false;
        var issuerPublicKey = didPublicKeyResolver.resolvePublicKey(issuer.getContent());
        var verificationResult = VerifiableCredentialFactory.verify(jwt, issuerPublicKey.getContent(), "verifiable-credential");
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
