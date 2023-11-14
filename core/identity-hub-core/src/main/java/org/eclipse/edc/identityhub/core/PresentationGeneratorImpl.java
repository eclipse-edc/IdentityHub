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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JSON_LD;

public class PresentationGeneratorImpl implements PresentationGenerator {
    private final CredentialFormat defaultFormatVp;
    private final PresentationCreatorRegistry registry;
    private final Monitor monitor;

    /**
     * Creates a PresentationGeneratorImpl object with the specified default formats for verifiable credentials and presentations.
     *
     * @param defaultFormatVp The default format for verifiable presentations.
     */
    public PresentationGeneratorImpl(CredentialFormat defaultFormatVp, PresentationCreatorRegistry registry, Monitor monitor) {
        this.defaultFormatVp = defaultFormatVp;
        this.registry = registry;
        this.monitor = monitor;
    }

    /**
     * Creates a presentation based on the given list of verifiable credentials and optional presentation definition. If the desired format ist {@link CredentialFormat#JSON_LD},
     * all JWT-VCs in the list will be packaged in a separate JWT VP, because LDP-VPs cannot contain JWT-VCs.
     * <em>Note: submitting a {@link PresentationDefinition} is not supported at the moment, and it will be ignored after logging a warning. </em>
     *
     * @param credentials            The list of verifiable credentials to include in the presentation.
     * @param presentationDefinition The optional presentation definition. <em>Not supported at the moment!</em>
     * @return A Result object wrapping the PresentationResponse.
     */
    @Override
    public Result<PresentationResponse> createPresentation(List<VerifiableCredentialContainer> credentials, @Nullable PresentationDefinition presentationDefinition) {

        if (presentationDefinition != null) {
            monitor.warning("A PresentationDefinition was submitted, but is currently ignored by the generator.");
        }
        var groups = credentials.stream().collect(Collectors.groupingBy(VerifiableCredentialContainer::format));
        var jwtVcs = ofNullable(groups.get(CredentialFormat.JWT)).orElseGet(List::of);
        var ldpVcs = ofNullable(groups.get(JSON_LD)).orElseGet(List::of);


        String vpToken;
        if (defaultFormatVp == JSON_LD) { // LDP-VPs cannot contain JWT VCs
            var arrayBuilder = Json.createArrayBuilder();
            if (!ldpVcs.isEmpty()) {
                JsonObject ldpVp = registry.createPresentation(ldpVcs, CredentialFormat.JSON_LD);
                arrayBuilder.add(ldpVp);
            }

            if (!jwtVcs.isEmpty()) {
                monitor.warning("The VP was requested in %s format, but the request yielded %s JWT-VCs, which cannot be transported in a LDP-VP. A second VP will be returned, containing JWT-VCs".formatted(JSON_LD, jwtVcs.size()));
                String jwtVp = registry.createPresentation(jwtVcs, CredentialFormat.JWT);
                arrayBuilder.add(jwtVp);
            }

            vpToken = arrayBuilder.build().toString();
        } else { //defaultFormatVp == JWT
            vpToken = registry.createPresentation(credentials, CredentialFormat.JWT);
        }

        var presentationResponse = new PresentationResponse(vpToken, null);
        return Result.success(presentationResponse);
    }
}
