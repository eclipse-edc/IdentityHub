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
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifierRegistry;
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

    private final CredentialEnvelopeVerifierRegistry credentialEnvelopeVerifierRegistry;

    /**
     * Create a new credential verifier that uses an Identity Hub
     *
     * @param identityHubClient                  IdentityHubClient.
     * @param credentialEnvelopeVerifierRegistry verifier registry
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient identityHubClient, Monitor monitor, CredentialEnvelopeVerifierRegistry credentialEnvelopeVerifierRegistry) {
        this.identityHubClient = identityHubClient;
        this.monitor = monitor;
        this.credentialEnvelopeVerifierRegistry = credentialEnvelopeVerifierRegistry;
    }

    /**
     * Returns a map of successfully verified Credential such as defined in <a href="https://www.w3.org/TR/vc-data-model/#credentials">W3C specification</a>.
     * The key of each map entry is the associated <a href="https://www.w3.org/TR/vc-data-model/#identifiers">credential identifier</a>.
     *
     * @param didDocument of a participant. The Did Document should contain an IdentityHub service.
     * @return verified credentials.
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
                .collect(Collectors.toMap(Credential::getId, credential -> (Object) credential));

        var verifiedCredentialsFailure = verifiedCredentialsResult
                .get(false)
                .stream()
                .map(Result::getFailureDetail)
                .collect(Collectors.toList());

        return new AggregatedResult<>(verifiedlCredentials, verifiedCredentialsFailure);

    }


    private Result<Credential> verifyCredential(CredentialEnvelope verifiableCredentials, DidDocument didDocument) {
        var verifier = credentialEnvelopeVerifierRegistry.resolve(verifiableCredentials.format());

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
