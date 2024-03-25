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

package org.eclipse.edc.identityhub.core.creators;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.edc.identityhub.core.creators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;
import static org.eclipse.edc.identityhub.core.creators.PresentationGeneratorConstants.VERIFIABLE_CREDENTIAL_PROPERTY;
import static org.eclipse.edc.identityhub.core.creators.PresentationGeneratorConstants.VP_TYPE_PROPERTY;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;

/**
 * JwtPresentationCreator is an implementation of the PresentationCreator interface that generates Verifiable Presentations in JWT format.
 * VPs are returned as {@link String}
 */
public class JwtPresentationGenerator implements PresentationGenerator<String> {
    public static final String VERIFIABLE_PRESENTATION_CLAIM = "vp";
    private final PrivateKeyResolver privateKeyResolver;
    private final Clock clock;
    private final String issuerId;

    private final TokenGenerationService tokenGenerationService;

    /**
     * Creates a JWT presentation based on a list of Verifiable Credential Containers.
     *
     * @param privateKeyResolver The resolver for private keys used for signing the presentation.
     * @param clock              The clock used for generating timestamps.
     * @param issuerId           The ID of the issuer for the presentation. Could be a DID.
     */
    public JwtPresentationGenerator(PrivateKeyResolver privateKeyResolver, Clock clock, String issuerId, TokenGenerationService tokenGenerationService) {
        this.privateKeyResolver = privateKeyResolver;
        this.clock = clock;
        this.issuerId = issuerId;
        this.tokenGenerationService = tokenGenerationService;
    }

    /**
     * Will always throw an {@link UnsupportedOperationException}.
     * Please use {@link JwtPresentationGenerator#generatePresentation(List, String, String, Map)} instead.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId) {
        throw new UnsupportedOperationException("Must provide additional data: '%s' and '%s'".formatted(AUDIENCE, CONTROLLER_ADDITIONAL_DATA));
    }

    /**
     * Creates a presentation using the given Verifiable Credential Containers and additional data.
     *
     * @param credentials     The list of Verifiable Credential Containers to include in the presentation.
     * @param privateKeyAlias The alias of the private key to be used for generating the presentation.
     * @param publicKeyId     The ID used by the counterparty to resolve the public key for verifying the VP.
     * @param additionalData  Additional data to include in the presentation. Must contain an entry 'aud'. Every entry in the map is added as a claim to the token.
     * @return The serialized JWT presentation.
     * @throws IllegalArgumentException      If the additional data does not contain the required 'aud' value or if no private key could be resolved for the key ID.
     * @throws UnsupportedOperationException If the private key does not provide any supported JWS algorithms.
     * @throws EdcException                  If signing the JWT fails.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId, Map<String, Object> additionalData) {

        // check if expected data is there
        if (!additionalData.containsKey(AUDIENCE)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(AUDIENCE));
        }

        if (!additionalData.containsKey(CONTROLLER_ADDITIONAL_DATA)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(CONTROLLER_ADDITIONAL_DATA));
        }

        var rawVcs = credentials.stream().map(VerifiableCredentialContainer::rawVc);
        Supplier<PrivateKey> privateKeySupplier = () -> privateKeyResolver.resolvePrivateKey(privateKeyAlias).orElseThrow(f -> new IllegalArgumentException(f.getFailureDetail()));
        var tokenResult = tokenGenerationService.generate(privateKeySupplier, vpDecorator(rawVcs), tp -> {
            additionalData.forEach(tp::claims);
            return tp;
        }, new KeyIdDecorator(additionalData.get(CONTROLLER_ADDITIONAL_DATA) + "#" + publicKeyId));

        return tokenResult.map(TokenRepresentation::getToken).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private TokenDecorator vpDecorator(Stream<String> rawVcs) {
        var now = Date.from(clock.instant());
        return tp -> tp.claims(ISSUER, issuerId)
                .claims(ISSUED_AT, now)
                .claims(NOT_BEFORE, now)
                .claims(JWT_ID, UUID.randomUUID().toString())
                .claims(VERIFIABLE_PRESENTATION_CLAIM, createVpClaim(rawVcs))
                .claims(EXPIRATION_TIME, Date.from(Instant.now().plusSeconds(60)));
    }

    private String createVpClaim(Stream<String> rawVcs) {
        var vcArray = Json.createArrayBuilder();
        rawVcs.forEach(vcArray::add);

        return Json.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, stringArray(List.of(IATP_CONTEXT_URL, W3C_CREDENTIALS_URL, PRESENTATION_EXCHANGE_URL)))
                .add(VP_TYPE_PROPERTY, VERIFIABLE_PRESENTATION_TYPE) // todo: add more types here?
                .add(VERIFIABLE_CREDENTIAL_PROPERTY, vcArray.build())
                .build()
                .toString();
    }

    private JsonArrayBuilder stringArray(Collection<?> values) {
        var ja = Json.createArrayBuilder();
        values.forEach(s -> ja.add(s.toString()));
        return ja;
    }
}
