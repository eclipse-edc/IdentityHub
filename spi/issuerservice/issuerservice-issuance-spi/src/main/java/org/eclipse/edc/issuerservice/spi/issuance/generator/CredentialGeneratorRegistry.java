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

package org.eclipse.edc.issuerservice.spi.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registry for credential generators based on {@link CredentialFormat}.
 */
@ExtensionPoint
public interface CredentialGeneratorRegistry {

    /**
     * Adds a generator for the given {@link CredentialFormat}.
     *
     * @param credentialFormat The credential format
     * @param generator        The generator
     */
    void addGenerator(CredentialFormat credentialFormat, CredentialGenerator generator);

    /**
     * Generates credentials based on the given {@link CredentialGenerationRequest}s and claims.
     *
     * @param issuerContextId              The issuer context ID
     * @param participantId                The participant ID
     * @param credentialGenerationRequests The credential generation requests
     * @param claims                       The claims to use for credential generation
     * @return The list of {@link VerifiableCredentialContainer}s if successful, or the failure information if unsuccessful
     */
    default Result<List<VerifiableCredentialContainer>> generateCredentials(String issuerContextId, String participantId, List<CredentialGenerationRequest> credentialGenerationRequests, Map<String, Object> claims) {
        var credentials = new ArrayList<VerifiableCredentialContainer>();
        for (var request : credentialGenerationRequests) {
            var result = generateCredential(issuerContextId, participantId, request, claims);
            if (result.succeeded()) {
                credentials.add(result.getContent());
            } else {
                return result.mapFailure();
            }
        }
        return Result.success(credentials);
    }

    /**
     * Generates a credential based on the given {@link CredentialGenerationRequest} and claims.
     *
     * @param issuerContextId             The issuer context ID
     * @param participantId               The participant ID
     * @param credentialGenerationRequest The credential generation request
     * @param claims                      The claims to use for credential generation
     * @return The {@link VerifiableCredentialContainer} if successful, or the failure information if unsuccessful
     */
    Result<VerifiableCredentialContainer> generateCredential(String issuerContextId, String participantId, CredentialGenerationRequest credentialGenerationRequest, Map<String, Object> claims);
}
