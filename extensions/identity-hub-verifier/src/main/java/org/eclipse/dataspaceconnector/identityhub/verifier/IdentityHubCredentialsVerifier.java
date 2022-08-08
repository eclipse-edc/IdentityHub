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

package org.eclipse.dataspaceconnector.identityhub.verifier;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.partitioningBy;

/**
 * Obtains and verifies credentials associated with a DID.
 * The DID document contains an IdentityHub service, the IdentityHubCredentialsVerifier gets credentials from the
 * IdentityHub instance and verifies the credentials.
 */
public class IdentityHubCredentialsVerifier implements CredentialsVerifier {

    private static final String IDENTITY_HUB_SERVICE_TYPE = "IdentityHub";
    private final IdentityHubClient identityHubClient;
    private final Monitor monitor;
    private final JwtCredentialsVerifier jwtCredentialsVerifier;
    private final VerifiableCredentialsJwtService verifiableCredentialsJwtService;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient IdentityHubClient.
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, JwtCredentialsVerifier jwtCredentialsVerifier, VerifiableCredentialsJwtService verifiableCredentialsJwtService) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.jwtCredentialsVerifier = jwtCredentialsVerifier;
        this.verifiableCredentialsJwtService = verifiableCredentialsJwtService;
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
        monitor.debug(() -> "Retrieving verified credentials for " + didDocument.getId());

        var hubBaseUrl = getIdentityHubBaseUrl(didDocument);
        if (hubBaseUrl == null) {
            var errorMessage = "Could not retrieve identity hub URL from DID document";
            monitor.severe(errorMessage);
            return Result.failure(errorMessage);
        }

        monitor.debug(() -> String.format("Using identity hub URL: %s", hubBaseUrl));

        var verifiableCredentials = identityHubClient.getVerifiableCredentials(hubBaseUrl);
        if (verifiableCredentials.failed()) {
            monitor.severe("Could not retrieve verifiable credentials from identity hub");
            return Result.failure(verifiableCredentials.getFailureMessages());
        }

        monitor.debug(() -> String.format("Retrieved %s verifiable credentials from identity hub", verifiableCredentials.getContent().size()));

        var verifiedCredentials = verifyCredentials(verifiableCredentials, didDocument);

        monitor.debug(() -> String.format("Verified %s credentials", verifiedCredentials.size()));

        var claims = extractClaimsFromCredential(verifiedCredentials);

        return Result.success(claims);
    }

    @NotNull
    private List<SignedJWT> verifyCredentials(StatusResult<Collection<SignedJWT>> jwts, DidDocument didDocument) {
        var result = jwts.getContent()
                .stream()
                .collect(partitioningBy((jwt) -> jwtCredentialsVerifier.verifyClaims(jwt, didDocument.getId()) && jwtCredentialsVerifier.isSignedByIssuer(jwt)));

        var successfulResults = result.get(true);
        var failedResults = result.get(false);

        if (!failedResults.isEmpty()) {
            monitor.warning(String.format("Ignoring %s invalid verifiable credentials", failedResults.size()));
        }

        return successfulResults;
    }

    @NotNull
    private Map<String, Object> extractClaimsFromCredential(List<SignedJWT> verifiedCredentials) {
        var result = verifiedCredentials.stream()
                .map(verifiableCredentialsJwtService::extractCredential)
                .collect(partitioningBy(AbstractResult::succeeded));

        var successfulResults = result.get(true);
        var failedResults = result.get(false);

        if (!failedResults.isEmpty()) {
            failedResults.forEach(f -> monitor.warning("Invalid credentials: " + f.getFailureDetail()));
        }

        return successfulResults.stream()
                .map(AbstractResult::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getIdentityHubBaseUrl(DidDocument didDocument) {
        return didDocument
                .getService()
                .stream()
                .filter(s -> s.getType().equals(IDENTITY_HUB_SERVICE_TYPE))
                .findFirst()
                .map(Service::getServiceEndpoint)
                .orElse(null);
    }
}
