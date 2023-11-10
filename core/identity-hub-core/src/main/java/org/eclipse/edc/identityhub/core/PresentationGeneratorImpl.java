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

package org.eclipse.edc.identityhub.core;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.PresentationResponse;
import org.eclipse.edc.identityhub.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.VERIFIABLE_PRESENTATION_TYPE;

public class PresentationGeneratorImpl implements PresentationGenerator {
    private final CredentialFormat defaultFormatVc;
    private final CredentialFormat defaultFormatVp;
    private final String credentialHolderId;
    private final PresentationCreatorRegistry registry;

    /**
     * Creates a PresentationGeneratorImpl object with the specified default formats for verifiable credentials and presentations.
     *
     * @param defaultFormatVc    The default format for verifiable credentials.
     * @param defaultFormatVp    The default format for verifiable presentations.
     * @param credentialHolderId The ID of the credential holder, for example a DID.
     */
    public PresentationGeneratorImpl(CredentialFormat defaultFormatVc, CredentialFormat defaultFormatVp, String credentialHolderId, PresentationCreatorRegistry registry) {
        this.defaultFormatVc = defaultFormatVc;
        this.defaultFormatVp = defaultFormatVp;
        this.credentialHolderId = credentialHolderId;
        this.registry = registry;
    }

    @Override
    public Result<PresentationResponse> createPresentation(List<VerifiableCredentialContainer> credentials, @Nullable PresentationDefinition presentationDefinition) {

        VerifiablePresentation.Builder.newInstance()
                .credentials(credentials.stream().map(VerifiableCredentialContainer::credential).toList())
                .type(VERIFIABLE_PRESENTATION_TYPE)
                .id(UUID.randomUUID().toString())
                .holder(credentialHolderId)
                .build();

        var groups = credentials.stream().collect(Collectors.groupingBy(VerifiableCredentialContainer::format));
        var jwtVcs = groups.get(CredentialFormat.JWT);
        var ldpVcs = groups.get(CredentialFormat.JSON_LD);

        JsonObject jwtVp = registry.createPresentation(jwtVcs, CredentialFormat.JWT);
        String ldpVp = registry.createPresentation(ldpVcs, CredentialFormat.JSON_LD);
        var vpToken = Json.createArrayBuilder().add(jwtVp).add(ldpVp).build();

        PresentationResponse presentationResponse = new PresentationResponse(vpToken.toString(), null);
        return Result.success(presentationResponse);
    }
}
