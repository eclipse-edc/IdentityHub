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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.PrivateKeyResolver;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;

/**
 * JwtPresentationCreator is an implementation of the PresentationCreator interface that generates Verifiable Presentations in JWT format.
 * VPs are returned as {@link String}
 */
public class JwtPresentationGenerator implements PresentationGenerator<String> {
    private final PrivateKeyResolver privateKeyResolver;
    private final Clock clock;
    private final String issuerId;

    /**
     * Creates a JWT presentation based on a list of Verifiable Credential Containers.
     *
     * @param privateKeyResolver The resolver for private keys used for signing the presentation.
     * @param clock              The clock used for generating timestamps.
     * @param issuerId           The ID of the issuer for the presentation. Could be a DID.
     */
    public JwtPresentationGenerator(PrivateKeyResolver privateKeyResolver, Clock clock, String issuerId) {
        this.privateKeyResolver = privateKeyResolver;
        this.clock = clock;
        this.issuerId = issuerId;
    }

    /**
     * Will always throw an {@link UnsupportedOperationException}.
     * Please use {@link JwtPresentationGenerator#generatePresentation(List, String, Map)} instead.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String keyId) {
        throw new UnsupportedOperationException("Must provide additional data: 'aud'");
    }

    /**
     * Creates a presentation using the given Verifiable Credential Containers and additional data.
     *
     * @param credentials    The list of Verifiable Credential Containers to include in the presentation.
     * @param keyId          The key ID of the private key to be used for generating the presentation.
     * @param additionalData Additional data to include in the presentation. Must contain an entry 'aud'. Every entry in the map is added as a claim to the token.
     * @return The serialized JWT presentation.
     * @throws IllegalArgumentException      If the additional data does not contain the required 'aud' value or if no private key could be resolved for the key ID.
     * @throws UnsupportedOperationException If the private key does not provide any supported JWS algorithms.
     * @throws EdcException                  If signing the JWT fails.
     */
    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String keyId, Map<String, Object> additionalData) {

        // check if expected data is there
        if (!additionalData.containsKey("aud")) {
            throw new IllegalArgumentException("Must provide additional data: 'aud'");
        }

        // check if private key can be resolved
        var pk = ofNullable(privateKeyResolver.resolvePrivateKey(keyId, PrivateKeyWrapper.class))
                .orElseThrow(() -> new IllegalArgumentException("No key could be found with key ID '%s'.".formatted(keyId)));

        var rawVcs = credentials.stream().map(VerifiableCredentialContainer::rawVc);
        var now = Date.from(clock.instant());
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuerId)
                .issueTime(now)
                .notBeforeTime(now)
                .jwtID(UUID.randomUUID().toString())
                .claim("vp", createVpClaim(rawVcs))
                .expirationTime(Date.from(Instant.now().plusSeconds(60)));

        additionalData.forEach(claimsSet::claim);

        var algo = pk.signer().supportedJWSAlgorithms().stream().findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Private key with ID '%s' did not provide any supported JWS algorithms.".formatted(keyId)));
        var signedJwt = new SignedJWT(new JWSHeader.Builder(algo).keyID(keyId).build(), claimsSet.build());

        try {
            signedJwt.sign(pk.signer());
        } catch (JOSEException e) {
            throw new EdcException(e);
        }

        return signedJwt.serialize();
    }

    private String createVpClaim(Stream<String> rawVcs) {
        var vcArray = Json.createArrayBuilder();
        rawVcs.forEach(vcArray::add);

        return Json.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, stringArray(List.of(IATP_CONTEXT_URL, W3C_CREDENTIALS_URL, PRESENTATION_EXCHANGE_URL)))
                .add("type", VERIFIABLE_PRESENTATION_TYPE) // todo: add more types here?
                .add("verifiableCredential", vcArray.build())
                .build()
                .toString();
    }

    private JsonArrayBuilder stringArray(Collection<?> values) {
        var ja = Json.createArrayBuilder();
        values.forEach(s -> ja.add(s.toString()));
        return ja;
    }
}
