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
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation;

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
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_LD;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.LdpPresentationGenerator.TYPE_ADDITIONAL_DATA;

public class VerifiablePresentationServiceImpl implements VerifiablePresentationService {
    private final PresentationCreatorRegistry registry;
    private final Monitor monitor;

    /**
     * Creates a PresentationGeneratorImpl object with the specified default formats for verifiable credentials and presentations.
     */
    public VerifiablePresentationServiceImpl(PresentationCreatorRegistry registry, Monitor monitor) {
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
        var jwt11Vcs = ofNullable(groups.get(VC1_0_JWT)).orElseGet(List::of);
        var ldp11Vcs = ofNullable(groups.get(VC1_0_LD)).orElseGet(List::of);
        var jwt20Vcs = ofNullable(groups.get(VC2_0_JOSE)).orElseGet(List::of);


        var vpToken = new ArrayList<>();
        var additionalDataJwt = new HashMap<String, Object>();
        ofNullable(audience).ifPresent(aud -> additionalDataJwt.put(AUDIENCE, audience));

        if (!jwt11Vcs.isEmpty()) {
            String jwt11Vp = registry.createPresentation(participantContextId, jwt11Vcs, VC1_0_JWT, additionalDataJwt);
            vpToken.add(jwt11Vp);
        }

        if (!ldp11Vcs.isEmpty()) {
            JsonObject ld11Vp = registry.createPresentation(participantContextId, ldp11Vcs, VC1_0_LD, Map.of(TYPE_ADDITIONAL_DATA, List.of(VERIFIABLE_PRESENTATION_TYPE)));
            vpToken.add(ld11Vp);
        }

        if (!jwt20Vcs.isEmpty()) {
            String jwt20Vp = registry.createPresentation(participantContextId, jwt20Vcs, VC2_0_JOSE, additionalDataJwt);
            vpToken.add(jwt20Vp);
        }

        var presentationResponse = PresentationResponseMessage.Builder.newinstance().presentation(vpToken).build();
        return Result.success(presentationResponse);
    }
}
