/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

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
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;

/**
 * Creates verifiable presentations according to Version 2.0 of the Verifiable Credential Data Model, that are secured
 * with an "enveloped proof" using the JOSE (= JWT) method.
 * <p>
 * This generator should be registered in the {@link org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry},
 * for the {@link CredentialFormat#VC2_0_JOSE} format.
 *
 * @see <a href="https://www.w3.org/TR/vc-jose-cose/#securing-vps-with-jose">Securing VPs with Enveloped Proofs using JOSE</a>
 */
public class JwtEnvelopedPresentationGenerator implements PresentationGenerator<String> {
    public static final String CONTEXT_PROPERTY = "@context";
    public static final String ID_PROPERTY = "id";
    public static final String TYPE_PROPERTY = "type";
    public static final String HOLDER_PROPERTY = "holder";
    public static final String VERIFIABLE_CREDENTIAL_PROPERTY = "verifiableCredential";
    public static final String VERIFIABLE_PRESENTATION_TYPE = "VerifiablePresentation";
    public static final String ENVELOPED_VERIFIABLE_CREDENTIAL = "EnvelopedVerifiableCredential";
    public static final String ENVELOPED_VERIFIABLE_PRESENTATION = "EnvelopedVerifiablePresentation";
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
        var keyIdDecorator = new KeyIdDecorator(composedKeyId);

        // create the enveloped VC JSON structure
        var rawVc = credentials.stream().map(VerifiableCredentialContainer::rawVc);
        var credentialArray = rawVc.map(vc -> Map.of(
                CONTEXT_PROPERTY, VC_PREFIX_V2,
                ID_PROPERTY, "data:application/vc+jwt,%s".formatted(vc),
                TYPE_PROPERTY, ENVELOPED_VERIFIABLE_CREDENTIAL
        )).toList();

        // create the payload for the VP token
        TokenDecorator presentationTokenGenerator = builder -> builder.claims(CONTEXT_PROPERTY, List.of(VC_PREFIX_V2))
                .claims(TYPE_PROPERTY, VERIFIABLE_PRESENTATION_TYPE)
                .claims(HOLDER_PROPERTY, issuerId)
                .claims(VERIFIABLE_CREDENTIAL_PROPERTY, credentialArray);

        // create the enveloped VP as JWT, signed again with the private key
        return tokenGenerationService.generate(privateKeyAlias, presentationTokenGenerator, keyIdDecorator)
                .compose(tr -> {
                    // create the enveloped presentation structure
                    TokenDecorator envelopedPresentationDecorator = builder -> builder.claims(ID_PROPERTY, "data:application/vp+jwt,%s".formatted(tr.getToken()))
                            .claims(TYPE_PROPERTY, ENVELOPED_VERIFIABLE_PRESENTATION)
                            .claims(CONTEXT_PROPERTY, List.of(VC_PREFIX_V2));
                    // ... and create a JWT out of it
                    return tokenGenerationService.generate(privateKeyAlias, envelopedPresentationDecorator, keyIdDecorator);
                })
                .map(TokenRepresentation::getToken)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
