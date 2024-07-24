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

package org.eclipse.edc.identithub.verifiablepresentation;

import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.JSON_LD;
import static org.eclipse.edc.identithub.verifiablepresentation.generators.LdpPresentationGenerator.TYPE_ADDITIONAL_DATA;

public class VerifiablePresentationServiceImpl implements VerifiablePresentationService {
    private final CredentialFormat defaultFormatVp;
    private final PresentationCreatorRegistry registry;
    private final Monitor monitor;

    /**
     * Creates a PresentationGeneratorImpl object with the specified default formats for verifiable credentials and presentations.
     *
     * @param defaultFormatVp The default format for verifiable presentations.
     */
    public VerifiablePresentationServiceImpl(CredentialFormat defaultFormatVp, PresentationCreatorRegistry registry, Monitor monitor) {
        this.defaultFormatVp = defaultFormatVp;
        this.registry = registry;
        this.monitor = monitor;
    }

    /**
     * Creates a presentation based on the given list of verifiable credentials and optional presentation definition. If the desired format ist {@link CredentialFormat#JSON_LD},
     * all JWT-VCs in the list will be packaged in a separate JWT VP, because LDP-VPs cannot contain JWT-VCs.
     * <em>Note: submitting a {@link PresentationDefinition} is not supported at the moment, and it will be ignored after logging a warning. </em>
     *
     * @param participantContextId   The ID of the {@link ParticipantContext} for which the VP is to be generated
     * @param credentials            The list of verifiable credentials to include in the presentation.
     * @param presentationDefinition The optional presentation definition. <em>Not supported at the moment!</em>
     * @param audience               The Participant ID of the entity who the VP is intended for. May be null for some VP formats.
     * @return A Result object wrapping the PresentationResponse.
     */
    @Override
    public Result<PresentationResponseMessage> createPresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, @Nullable PresentationDefinition presentationDefinition, @Nullable String audience) {

        if (presentationDefinition != null) {
            monitor.warning("A PresentationDefinition was submitted, but is currently ignored by the generator.");
        }
        var groups = credentials.stream().collect(Collectors.groupingBy(VerifiableCredentialContainer::format));
        var jwtVcs = ofNullable(groups.get(CredentialFormat.JWT)).orElseGet(List::of);
        var ldpVcs = ofNullable(groups.get(JSON_LD)).orElseGet(List::of);


        var vpToken = new ArrayList<>();

        var additionalDataJwt = new HashMap<String, Object>();
        ofNullable(audience).ifPresent(aud -> additionalDataJwt.put(AUDIENCE, audience));

        if (defaultFormatVp == JSON_LD) { // LDP-VPs cannot contain JWT VCs
            if (!ldpVcs.isEmpty()) {

                // todo: once we support PresentationDefinition, the types list could be dynamic
                JsonObject ldpVp = registry.createPresentation(participantContextId, ldpVcs, JSON_LD, Map.of(
                        TYPE_ADDITIONAL_DATA, List.of(VERIFIABLE_PRESENTATION_TYPE)));
                vpToken.add(ldpVp);
            }

            if (!jwtVcs.isEmpty()) {
                monitor.warning("The VP was requested in %s format, but the request yielded %s JWT-VCs, which cannot be transported in a LDP-VP. A second VP will be returned, containing JWT-VCs".formatted(JSON_LD, jwtVcs.size()));
                String jwtVp = registry.createPresentation(participantContextId, jwtVcs, CredentialFormat.JWT, additionalDataJwt);
                vpToken.add(jwtVp);
            }

        } else { //defaultFormatVp == JWT
            vpToken.add(registry.createPresentation(participantContextId, credentials, CredentialFormat.JWT, additionalDataJwt));
        }

        var presentationResponse = PresentationResponseMessage.Builder.newinstance().presentation(vpToken).build();
        return Result.success(presentationResponse);
    }
}
