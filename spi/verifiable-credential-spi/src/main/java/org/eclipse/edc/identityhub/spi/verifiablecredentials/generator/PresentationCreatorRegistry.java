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


import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;

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
     * @param <T>                  The type of the presentation. Can be {@link String}, when format is {@link CredentialFormat#VC1_0_JWT}, or {@link jakarta.json.JsonObject},
     *                             when the format is {@link CredentialFormat#VC1_0_LD}
     * @param participantContextId The ID of the {@code ParticipantContext} who creates the VP
     * @param credentials          The list of verifiable credentials to include in the presentation.
     * @param format               The format for the presentation.
     * @param additionalData       Optional additional data that might be required to create the presentation, such as types, etc.
     * @return The created presentation.
     * @throws IllegalArgumentException         if the credential cannot be represented in the desired format. For example, LDP-VPs cannot contain JWT-VCs.
     * @throws org.eclipse.edc.spi.EdcException if no creator is registered for a particular format
     */
    <T> T createPresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, CredentialFormat format, Map<String, Object> additionalData);
}
