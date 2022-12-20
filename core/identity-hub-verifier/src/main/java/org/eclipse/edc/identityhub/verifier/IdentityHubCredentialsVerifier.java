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

package org.eclipse.edc.identityhub.verifier;

import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialVerifierRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
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

    private final CredentialVerifierRegistry credentialVerifierRegistry;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient          IdentityHubClient.
     * @param credentialVerifierRegistry
     *
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, CredentialVerifierRegistry credentialVerifierRegistry) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.credentialVerifierRegistry = credentialVerifierRegistry;
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

        monitor.debug(() -> format("Using identity hub URL: %s", hubBaseUrl));

        var verifiableCredentials = identityHubClient.getVerifiableCredentials(hubBaseUrl);
        if (verifiableCredentials.failed()) {
            monitor.severe("Could not retrieve verifiable credentials from identity hub");
            return Result.failure(verifiableCredentials.getFailureMessages());
        }

        monitor.debug(() -> format("Retrieved %s verifiable credentials from identity hub", verifiableCredentials.getContent().size()));

        var claims = verifyCredentials(verifiableCredentials.getContent(), didDocument);

        var result = new AggregatedResult<>(claims.getContent(), claims.getFailureMessages());

        // Fail if one verifiable credential is not valid. This is a temporary solution until the CredentialsVerifier
        // contract is changed to support a result containing both successful results and failures.
        if (result.failed()) {
            monitor.severe(() -> format("Credentials verification failed: %s", result.getFailureDetail()));
            return Result.failure(result.getFailureDetail());
        } else {
            return Result.success(result.getContent());
        }
    }

    private AggregatedResult<Map<String, Object>> verifyCredentials(Collection<CredentialEnvelope> verifiableCredentials, DidDocument didDocument) {
        var verifiedCredentialsResult = verifiableCredentials
                .stream()
                .map(credentials -> verifyCredential(credentials, didDocument))
                .collect(partitioningBy(Result::succeeded));

        var verifiedlCredentials = verifiedCredentialsResult.get(true)
                .stream()
                .map(Result::getContent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var verifiedCredentialsFailure = verifiedCredentialsResult
                .get(false)
                .stream()
                .map(Result::getFailureDetail)
                .collect(Collectors.toList());

        return new AggregatedResult<>(verifiedlCredentials, verifiedCredentialsFailure);

    }


    private Result<Map.Entry<String, Object>> verifyCredential(CredentialEnvelope verifiableCredentials, DidDocument didDocument) {
        var verifier = credentialVerifierRegistry.resolve(verifiableCredentials.format());

        if (verifier == null) {
            return Result.failure(format("Missing verifier for credentials with format %s", verifiableCredentials.format()));
        }
        return verifier.verify(verifiableCredentials, didDocument);
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
