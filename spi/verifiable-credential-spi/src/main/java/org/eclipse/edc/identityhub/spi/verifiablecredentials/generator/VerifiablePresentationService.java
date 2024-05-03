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


import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a Presentation Generator that creates a presentation based on a list of verifiable credentials
 * and an optional presentation definition.
 */
@FunctionalInterface
public interface VerifiablePresentationService {
    /**
     * Creates a presentation based on a list of verifiable credentials and an optional presentation definition.
     *
     * @param participantContextId   The ID or the {@code ParticipantContext} for whom a VerifiablePresentation is to be created
     * @param credentials            The list of verifiable credentials to include in the presentation.
     * @param presentationDefinition The optional presentation definition.
     * @param audience               The Participant ID of the party who the presentation is intended for. May not be relevant for all VP formats
     * @return A Result object containing a PresentationResponse if the presentation creation is successful, or a failure message if it fails.
     */
    Result<PresentationResponseMessage> createPresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, @Nullable PresentationDefinition presentationDefinition, @Nullable String audience);
}
