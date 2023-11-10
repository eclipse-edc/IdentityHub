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

import org.eclipse.edc.identityhub.spi.model.PresentationResponse;
import org.eclipse.edc.identityhub.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a Presentation Generator that creates a presentation based on a list of verifiable credentials
 * and an optional presentation definition.
 */
@FunctionalInterface
public interface PresentationGenerator {
    /**
     * Creates a presentation based on a list of verifiable credentials and an optional presentation definition.
     *
     * @param credentials            The list of verifiable credentials to include in the presentation.
     * @param presentationDefinition The optional presentation definition.
     * @return A Result object containing a PresentationResponse if the presentation creation is successful,
     * or a failure message if it fails.
     */
    Result<PresentationResponse> createPresentation(List<VerifiableCredentialContainer> credentials, @Nullable PresentationDefinition presentationDefinition);
}
