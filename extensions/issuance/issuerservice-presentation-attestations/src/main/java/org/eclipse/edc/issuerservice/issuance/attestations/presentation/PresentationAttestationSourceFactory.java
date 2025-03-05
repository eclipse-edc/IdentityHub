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

package org.eclipse.edc.issuerservice.issuance.attestations.presentation;


import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;

/**
 * Creates an attestation source that requires a verifiable credential.
 * <p>
 * Example configuration is:
 * <pre>
 * {
 *   "id": "123",
 *   "attestationType": "presentation",
 *   "configuration": {
 *     "credentialType": "https://example.com/ExampleCredential",
 *     "required": true,
 *     "outputClaim": "exampleCredential"
 *   }
 * }
 * </pre>
 */
public class PresentationAttestationSourceFactory implements AttestationSourceFactory {
    private static final String CREDENTIAL_TYPE = "credentialType";
    private static final String OUTPUT_CLAIM = "outputClaim";
    private static final String REQUIRED = "required";

    @Override
    public AttestationSource createSource(AttestationDefinition definition) {
        var configuration = definition.getConfiguration();
        var credentialType = (String) configuration.get(CREDENTIAL_TYPE);
        var outputClaim = (String) configuration.get(OUTPUT_CLAIM);
        var required = (Boolean) configuration.getOrDefault(REQUIRED, true);
        return new PresentationAttestationSource(credentialType, outputClaim, required);
    }
}
