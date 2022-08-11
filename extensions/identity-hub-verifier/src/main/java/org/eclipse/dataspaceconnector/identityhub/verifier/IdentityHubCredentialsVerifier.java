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
import java.util.stream.Stream;

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
     * Here an example of the format returned by this method:
     * <pre>{@code
     * {
     *     "vc_id_1": {
     *         "vc": {
     *           "id": "vc_id_1",
     *           "credentialSubject": {
     *             "region": "eu"
     *           },
     *            "iss": "did:web:issuer1",
     *            "sub": "did:web:subjectA"
     *         }
     *       }
     *   }
     *   }</pre>
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

        monitor.debug(() -> String.format("Verified %s credentials", verifiedCredentials.getContent().size()));

        var claims = extractClaimsFromCredential(verifiedCredentials.getContent());

        var failureMessages = mergeFailureMessages(verifiedCredentials.getFailureMessages(), claims.getFailureMessages());

        var result = new AggregatedResult<>(claims.getContent(), failureMessages);

        // Fail if one verifiable credential is not valid. This is a temporary solution until the CredentialsVerifier
        // contract is changed to support a result containing both successful results and failures.
        if (result.failed()) {
            monitor.severe(() -> String.format("Credentials verification failed: %s", claims.getFailureDetail()));
            return Result.failure(result.getFailureDetail());
        } else {
            return Result.success(result.getContent());
        }
    }

    @NotNull
    private List<String> mergeFailureMessages(Collection<String> messages, Collection<String> otherMessages) {
        return Stream.concat(messages.stream(), otherMessages.stream()).collect(Collectors.toList());
    }

    @NotNull
    private AggregatedResult<List<SignedJWT>> verifyCredentials(StatusResult<Collection<SignedJWT>> jwts, DidDocument didDocument) {
        // Get valid credentials.
        var verifiedJwts = jwts.getContent()
                .stream()
                .map(jwt -> verifyJwtClaims(jwt, didDocument))
                .collect(partitioningBy(AbstractResult::succeeded));

        var jwtsSignedByIssuer = verifiedJwts.get(true)
                .stream()
                .map(jwt -> verifySignature(jwt.getContent()))
                .collect(partitioningBy(AbstractResult::succeeded));

        var validCredentials = jwtsSignedByIssuer
                .get(true)
                .stream()
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

        // Gather failure messages of invalid credentials.
        var verificationFailures = verifiedJwts
                .get(false)
                .stream()
                .map(AbstractResult::getFailureDetail);

        var signatureVerificationFailures = jwtsSignedByIssuer
                .get(false)
                .stream()
                .map(AbstractResult::getFailureDetail);

        var failedResults = Stream.concat(verificationFailures, signatureVerificationFailures)
                .collect(Collectors.toList());

        if (!failedResults.isEmpty()) {
            monitor.warning(String.format("Found %s invalid verifiable credentials", failedResults.size()));
        }

        return new AggregatedResult<>(validCredentials, failedResults);
    }

    @NotNull
    private Result<SignedJWT> verifyJwtClaims(SignedJWT jwt, DidDocument didDocument) {
        var result = jwtCredentialsVerifier.verifyClaims(jwt, didDocument.getId());
        return result.succeeded() ? Result.success(jwt) : Result.failure(result.getFailureMessages());
    }

    @NotNull
    private Result<SignedJWT> verifySignature(SignedJWT jwt) {
        var result = jwtCredentialsVerifier.isSignedByIssuer(jwt);
        return result.succeeded() ? Result.success(jwt) : Result.failure(result.getFailureMessages());
    }

    @NotNull
    private AggregatedResult<Map<String, Object>> extractClaimsFromCredential(List<SignedJWT> verifiedCredentials) {
        var result = verifiedCredentials.stream()
                .map(verifiableCredentialsJwtService::extractCredential)
                .collect(partitioningBy(AbstractResult::succeeded));

        var successfulResults = result.get(true).stream()
                .map(AbstractResult::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var failedResults = result.get(false).stream()
                .map(AbstractResult::getFailureDetail)
                .collect(Collectors.toList());

        return new AggregatedResult<>(successfulResults, failedResults);
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
