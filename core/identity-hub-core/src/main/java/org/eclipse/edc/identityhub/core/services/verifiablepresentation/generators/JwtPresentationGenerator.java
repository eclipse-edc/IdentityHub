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

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import org.eclipse.edc.iam.identitytrust.spi.DcpConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.VERIFIABLE_CREDENTIAL_PROPERTY;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.VP_TYPE_PROPERTY;

/**
 * JwtPresentationCreator is an implementation of the PresentationCreator interface that generates Verifiable Presentations in JWT format.
 * VPs are returned as {@link String}
 */
public class JwtPresentationGenerator implements PresentationGenerator<String> {
    public static final String VERIFIABLE_PRESENTATION_CLAIM = "vp";
    private final Clock clock;

    private final TokenGenerationService tokenGenerationService;

    /**
     * Creates a JWT presentation based on a list of Verifiable Credential Containers.
     *
     * @param clock                  The clock used for generating timestamps.
     * @param tokenGenerationService service to generate (JWT) tokens
     */
    public JwtPresentationGenerator(Clock clock, TokenGenerationService tokenGenerationService) {
        this.clock = clock;
        this.tokenGenerationService = tokenGenerationService;
    }

    /**
     * Will always throw an {@link UnsupportedOperationException}.
     * Please use {@link PresentationGenerator#generatePresentation(List, String, String, String, Map)} instead.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId) {
        throw new UnsupportedOperationException("Must provide additional data: '%s' and '%s'".formatted(JwtRegisteredClaimNames.AUDIENCE, CONTROLLER_ADDITIONAL_DATA));
    }

    /**
     * Creates a presentation using the given Verifiable Credential Containers and additional data.
     *
     * @param credentials     The list of Verifiable Credential Containers to include in the presentation.
     * @param privateKeyAlias The alias of the private key to be used for generating the presentation.
     * @param publicKeyId     The ID used by the counterparty to resolve the public key for verifying the VP.
     * @param issuerId        The ID of this issuer. Usually a DID.
     * @param additionalData  Additional data to include in the presentation. Must contain an entry 'aud'. Every entry in the map is added as a claim to the token.
     * @return The serialized JWT presentation.
     * @throws IllegalArgumentException      If the additional data does not contain the required 'aud' value or if no private key could be resolved for the key ID.
     * @throws UnsupportedOperationException If the private key does not provide any supported JWS algorithms.
     * @throws EdcException                  If signing the JWT fails.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId, String issuerId, Map<String, Object> additionalData) {

        // check if expected data is there
        if (!additionalData.containsKey(JwtRegisteredClaimNames.AUDIENCE)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(JwtRegisteredClaimNames.AUDIENCE));
        }

        if (!additionalData.containsKey(CONTROLLER_ADDITIONAL_DATA)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(CONTROLLER_ADDITIONAL_DATA));
        }

        var controller = additionalData.get(CONTROLLER_ADDITIONAL_DATA).toString();
        var composedKeyId = publicKeyId;
        if (!publicKeyId.startsWith(controller)) {
            composedKeyId = controller + "#" + publicKeyId;
        }

        var rawVcs = credentials.stream()
                .map(VerifiableCredentialContainer::rawVc)
                .toList();
        var tokenResult = tokenGenerationService.generate(privateKeyAlias, vpDecorator(rawVcs, issuerId), tp -> {
            additionalData.forEach(tp::claims);
            return tp;
        }, new KeyIdDecorator(composedKeyId));

        return tokenResult.map(TokenRepresentation::getToken).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private TokenDecorator vpDecorator(List<String> rawVcs, String issuerId) {
        var now = Date.from(clock.instant());
        return tp -> tp.claims(JwtRegisteredClaimNames.ISSUER, issuerId)
                .claims(JwtRegisteredClaimNames.ISSUED_AT, now)
                .claims(JwtRegisteredClaimNames.NOT_BEFORE, now)
                .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString())
                .claims(VERIFIABLE_PRESENTATION_CLAIM, createVpClaim(rawVcs))
                .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Date.from(Instant.now().plusSeconds(60)));
    }

    private Map<String, Object> createVpClaim(List<String> rawVcs) {
        return Map.of(
                JsonLdKeywords.CONTEXT, List.of(DcpConstants.DCP_CONTEXT_URL, VcConstants.W3C_CREDENTIALS_URL, VcConstants.PRESENTATION_EXCHANGE_URL),
                VP_TYPE_PROPERTY, VcConstants.VERIFIABLE_PRESENTATION_TYPE,
                VERIFIABLE_CREDENTIAL_PROPERTY, rawVcs
        );
    }
}
