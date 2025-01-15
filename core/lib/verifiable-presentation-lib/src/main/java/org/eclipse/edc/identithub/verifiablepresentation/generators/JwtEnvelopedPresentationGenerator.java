/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.identithub.verifiablepresentation.generators;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.identithub.verifiablepresentation.generators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;

/**
 * Creates verifiable presentations that are secured with an "enveloped proof" using the JOSE (= JWT) method.
 *
 * @see <a href="https://www.w3.org/TR/vc-jose-cose/#securing-vps-with-jose">Securing VPs with Enveloped Proofs using JOSE</a>
 */
public class JwtEnvelopedPresentationGenerator implements PresentationGenerator<String> {
    private final Monitor monitor;
    private final TokenGenerationService tokenGenerationService;

    public JwtEnvelopedPresentationGenerator(Monitor monitor, TokenGenerationService tokenGenerationService) {
        this.monitor = monitor;
        this.tokenGenerationService = tokenGenerationService;
    }

    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId) {
        throw new UnsupportedOperationException("Must provide additional data: '%s'".formatted(CONTROLLER_ADDITIONAL_DATA));
    }

    @Override
    public String generatePresentation(List<VerifiableCredentialContainer> credentials, String privateKeyAlias, String publicKeyId, String issuerId, Map<String, Object> additionalData) {
        var violatingCredentials = credentials.stream().filter(vc -> vc.format() != CredentialFormat.VC2_0_JOSE).toList();

        if (!violatingCredentials.isEmpty()) {
            var violations = violatingCredentials.stream().map(vc -> "%s -> %s".formatted(vc.credential().getId(), vc.format())).toList();
            var msg = "The %s can only handle credentials that are in %s format, but the following credentials are in a different format: %s".formatted(getClass().getSimpleName(), CredentialFormat.VC2_0_JOSE, violations);
            monitor.severe(msg);
            throw new IllegalArgumentException(msg);
        }

        //noinspection DuplicatedCode
        if (!additionalData.containsKey(CONTROLLER_ADDITIONAL_DATA)) {
            throw new IllegalArgumentException("Must provide additional data: '%s'".formatted(CONTROLLER_ADDITIONAL_DATA));
        }

        var controller = additionalData.get(CONTROLLER_ADDITIONAL_DATA).toString();
        var composedKeyId = publicKeyId;
        if (!publicKeyId.startsWith(controller)) {
            composedKeyId = controller + "#" + publicKeyId;
        }

        // create the presentation structure
        var rawVcs = credentials.stream().map(VerifiableCredentialContainer::rawVc);
        var credentialArray = rawVcs.map(vc -> Map.of(
                "@context", VC_PREFIX_V2,
                "id", "data:application/vc+jwt,%s".formatted(vc),
                "type", "EnvelopedVerifiableCredential"
        )).toList();


        TokenDecorator presentationTokenGenerator = builder -> {
            builder.claims("@context", List.of(VC_PREFIX_V2))
                    .claims("type", "VerifiablePresentation")
                    .claims("holder", issuerId)
                    .claims("verifiableCredential", credentialArray);
            return builder;
        };

        var keyIdDecorator = new KeyIdDecorator(composedKeyId);
        return tokenGenerationService.generate(privateKeyAlias, presentationTokenGenerator, keyIdDecorator)
                .compose(tr -> {
                    // create the enveloped presentation structure
                    TokenDecorator envelopedPresentationDecorator = builder -> {
                        builder.claims("id", "data:application/vp+jwt,%s".formatted(tr.getToken()))
                                .claims("type", "EnvelopedVerifiablePresentation")
                                .claims("@context", List.of("https://www.w3.org/ns/credentials/v2"));
                        return builder;
                    };

                    return tokenGenerationService.generate(privateKeyAlias, envelopedPresentationDecorator, keyIdDecorator);
                })
                .map(TokenRepresentation::getToken)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
