/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.generator;


import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;

import java.util.List;
import java.util.Map;

/**
 * Generator interface for creating VerifiablePresentation objects out of {@link VerifiableCredentialContainer}s
 *
 * @param <T> the type of the presentation
 */
@FunctionalInterface
public interface PresentationGenerator<T> {

    /**
     * Generates a Verifiable Presentation based on a list of Verifiable Credential Containers and a key ID. Implementors must
     * use the key ID to resolve the private key used for signing. Recipients of the VP must use the key ID to resolve the public
     * key for verification. How the public key is made available is out-of-scope here, but a popular method is DID documents.
     * <p>
     * Implementors must check whether all VCs can be represented in one <em>one</em> VP, and if not, must throw a {@link IllegalArgumentException}.
     * <p>
     * The concrete return type of the VP depends on the implementation, for example JWT VPs are represented as String, LDP VPs are represented
     * as {@link jakarta.json.JsonObject}.
     *
     * @param credentials     The list of Verifiable Credential Containers to include in the presentation.
     * @param privateKeyAlias The alias of the private key to be used for generating the presentation.
     * @param publicKeyId     The ID used by the counterparty to resolve the public key for verifying the VP.
     * @return The generated Verifiable Presentation. The concrete return type depends on the implementation.
     * @throws IllegalArgumentException      If not all VCs can be represented in one VP.
     * @throws UnsupportedOperationException If additional data is required by the implementation, or if specified key is not suitable for signing.
     */
    T generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId);

    /**
     * Generates a Verifiable Presentation based on a list of Verifiable Credential Containers and a key ID. Implementors must
     * use the key ID to resolve the private key used for signing. Recipients of the VP must use the key ID to resolve the public
     * key for verification. How the public key is made available is out-of-scope here, but a popular method is DID documents.
     * <p>
     * Implementors must check whether all VCs can be represented in one <em>one</em> VP, and if not, must throw a {@link IllegalArgumentException}.
     * <p>
     * The concrete return type of the VP depends on the implementation, for example JWT VPs are represented as String, LDP VPs are represented
     * as {@link jakarta.json.JsonObject}.
     *
     * @param credentials     The list of Verifiable Credential Containers to include in the presentation.
     * @param privateKeyAlias The alias of the private key to be used for generating the presentation.
     * @param publicKeyId     The ID used by the counterparty to resolve the public key for verifying the VP.
     * @param issuerId        The ID of the issuer. Typically, this is a DID, NOT the participant ID!
     * @param additionalData  Additional data used for validation.
     * @return The generated Verifiable Presentation. The concrete return type depends on the implementation.
     * @throws IllegalArgumentException If not all VCs can be represented in one VP, mandatory additional information was not given, or the specified key is not suitable for signing.
     */
    default T generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId, String issuerId, Map<String, Object> additionalData) {
        return generatePresentation(credentials, privateKeyAlias, publicKeyId);
    }
}
