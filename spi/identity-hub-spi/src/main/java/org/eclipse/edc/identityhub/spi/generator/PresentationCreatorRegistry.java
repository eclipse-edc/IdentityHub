/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.generator;

import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;

import java.util.List;
import java.util.Map;

/**
 * Registry that contains multiple {@link PresentationGenerator} objects and assigns them a {@link CredentialFormat}.
 * With this, it is possible to generate VerifiablePresentations in different formats.
 */
public interface PresentationCreatorRegistry {

    /**
     * Registers a {@link PresentationGenerator} for a particular {@link CredentialFormat}
     */
    void addCreator(PresentationGenerator<?> creator, CredentialFormat format);

    /**
     * Creates a VerifiablePresentation based on a list of verifiable credentials and a credential format. How the presentation will be represented
     * depends on the format. JWT-VPs will be represented as {@link String}, LDP-VPs will be represented as {@link jakarta.json.JsonObject}.
     *
     * @param <T>            The type of the presentation. Can be {@link String}, when format is {@link CredentialFormat#JWT}, or {@link jakarta.json.JsonObject},
     *                       when the format is {@link CredentialFormat#JSON_LD}
     * @param credentials    The list of verifiable credentials to include in the presentation.
     * @param format         The format for the presentation.
     * @param additionalData Optional additional data that might be required to create the presentation, such as types, etc.
     * @return The created presentation.
     * @throws IllegalArgumentException         if the credential cannot be represented in the desired format. For example, LDP-VPs cannot contain JWT-VCs.
     * @throws org.eclipse.edc.spi.EdcException if no creator is registered for a particular format
     */
    <T> T createPresentation(List<VerifiableCredentialContainer> credentials, CredentialFormat format, Map<String, Object> additionalData);

    /**
     * Specify, which key ID is to be used for which {@link CredentialFormat}. It is recommended to use a separate key for every format.
     * The keyId must be the fully qualified ID, for example in DIDs one would have to pass {@code did:web:some-identifier#key-1}.
     *
     * @param keyId  the Key ID of the private key. Typically, the related public key has to be resolvable through a public method, e.g. DID:WEB
     * @param format the {@link CredentialFormat} for which the key should be used.
     */
    void addKeyId(String keyId, CredentialFormat format);
}
