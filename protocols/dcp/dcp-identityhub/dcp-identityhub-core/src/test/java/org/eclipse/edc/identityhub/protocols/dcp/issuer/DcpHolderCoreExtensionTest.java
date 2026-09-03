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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpHolderCoreExtensionTest {

    private static final String HOLDER_DID = "did:web:holder";
    private static final String ISSUER_DID = "did:web:issuer";

    private final TokenValidationService tokenValidationService = mock();

    private final IdentityHubParticipantContext participantContext = IdentityHubParticipantContext.Builder.newInstance()
            .participantContextId("holder-context")
            .did(HOLDER_DID)
            .apiTokenAlias("apiAlias")
            .build();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationService.class, tokenValidationService);
    }

    // TOK-09/TOK-11: the signing key is resolved from the DID named in the 'kid' header, so a token whose 'kid' points at
    // a different DID than the 'iss' claim must not authenticate as that issuer
    @Test
    @DisplayName("TOK-09: a token whose 'kid' names a verification method outside the 'iss' DID document is rejected")
    void createTokenVerifier_kidFromForeignDid_isRejected(ServiceExtensionContext context, ObjectFactory factory) throws JOSEException {
        var extension = factory.constructInstance(DcpHolderCoreExtension.class);
        extension.initialize(context);
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenAnswer(invocation -> Result.success(ClaimToken.Builder.newInstance()
                        .claim("iss", ISSUER_DID)
                        .build()));

        // signed with an attacker's key, but claiming to come from the issuer
        var token = signedToken(ISSUER_DID, "did:web:attacker#key1");
        extension.createTokenVerifier().verify(participantContext, TokenRepresentation.Builder.newInstance().token(token).build());

        var rulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tokenValidationService).validate(anyString(), any(), rulesCaptor.capture());
        @SuppressWarnings("unchecked")
        var rules = (List<TokenValidationRule>) rulesCaptor.getValue();
        var claims = ClaimToken.Builder.newInstance().claim("iss", ISSUER_DID).build();
        assertThat(keyBindingRule(rules).checkRule(claims, null).failed())
                .as("the key binding rule must reject a 'kid' that does not belong to the 'iss' DID")
                .isTrue();
    }

    @Test
    @DisplayName("TOK-09: a token whose 'kid' belongs to the 'iss' DID passes the key binding rule")
    void createTokenVerifier_kidFromIssuerDid_passesBinding(ServiceExtensionContext context, ObjectFactory factory) throws JOSEException {
        var extension = factory.constructInstance(DcpHolderCoreExtension.class);
        extension.initialize(context);
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenAnswer(invocation -> Result.success(ClaimToken.Builder.newInstance().build()));

        var token = signedToken(ISSUER_DID, ISSUER_DID + "#key1");
        extension.createTokenVerifier().verify(participantContext, TokenRepresentation.Builder.newInstance().token(token).build());

        var rulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tokenValidationService).validate(anyString(), any(), rulesCaptor.capture());
        @SuppressWarnings("unchecked")
        var rules = (List<TokenValidationRule>) rulesCaptor.getValue();
        var claims = ClaimToken.Builder.newInstance().claim("iss", ISSUER_DID).build();
        assertThat(keyBindingRule(rules).checkRule(claims, null).succeeded()).isTrue();
    }

    // a token without a 'kid' cannot be bound to its issuer at all, so it never reaches validation
    @Test
    @DisplayName("TOK-11: a token without a 'kid' header is rejected before validation")
    void createTokenVerifier_withoutKid_isRejected(ServiceExtensionContext context, ObjectFactory factory) throws JOSEException {
        var extension = factory.constructInstance(DcpHolderCoreExtension.class);
        extension.initialize(context);

        var token = signedToken(ISSUER_DID, null);
        var result = extension.createTokenVerifier().verify(participantContext, TokenRepresentation.Builder.newInstance().token(token).build());

        assertThat(result.failed()).isTrue();
        verifyNoInteractions(tokenValidationService);
    }

    private TokenValidationRule keyBindingRule(List<TokenValidationRule> rules) {
        return rules.stream()
                .filter(IssuerKeyIdValidationRule.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no key binding rule was applied"));
    }

    private String signedToken(String issuer, String kid) throws JOSEException {
        var key = new ECKeyGenerator(Curve.P_256).keyID("key1").generate();
        var headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256);
        if (kid != null) {
            headerBuilder.keyID(kid);
        }
        var jwt = new SignedJWT(headerBuilder.build(), new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(issuer)
                .audience(HOLDER_DID)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build());
        jwt.sign(new ECDSASigner(key.toECPrivateKey()));
        return jwt.serialize();
    }
}
