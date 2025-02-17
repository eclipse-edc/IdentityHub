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

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtCredentialGenerator implements CredentialGenerator {

    public static final String VERIFIABLE_CREDENTIAL_CLAIM = "vc";
    public static final String CREDENTIAL_SUBJECT = "credentialSubject";
    public static final String VERIFIABLE_CREDENTIAL = "VerifiableCredential";
    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;

    public JwtCredentialGenerator(TokenGenerationService tokenGenerationService, Clock clock) {
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(CredentialDefinition definition, String privateKeyAlias, String publicKeyId, String issuerId, String participantId, Map<String, Object> claims) {

        var subjectResult = getCredentialSubject(claims);

        if (subjectResult.failed()) {
            return subjectResult.mapFailure();
        }

        var credential = generateVerifiableCredential(definition.getCredentialType(), definition.getValidity(), issuerId, participantId, subjectResult.getContent());

        var composedKeyId = publicKeyId;
        if (!publicKeyId.startsWith(issuerId)) {
            composedKeyId = issuerId + "#" + publicKeyId;
        }

        return tokenGenerationService.generate(privateKeyAlias, vcDecorator(definition.getCredentialType(), participantId, credential), new KeyIdDecorator(composedKeyId))
                .map(token -> new VerifiableCredentialContainer(token.getToken(), CredentialFormat.VC1_0_JWT, credential));
    }

    @SuppressWarnings("unchecked")
    private VerifiableCredential generateVerifiableCredential(String type, long validity, String issuer, String participantId, Map<String, Object> credentialSubject) {
        return VerifiableCredential.Builder.newInstance()
                .issuer(new Issuer(issuer))
                .dataModelVersion(DataModelVersion.V_1_1)
                .issuanceDate(Instant.now(clock))
                .expirationDate(Instant.now(clock).plusSeconds(validity))
                .types(List.of(VERIFIABLE_CREDENTIAL, type))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(participantId)
                        .claims(credentialSubject)
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Result<Map<String, Object>> getCredentialSubject(Map<String, Object> claims) {
        if (!claims.containsKey(CREDENTIAL_SUBJECT)) {
            return Result.failure("Missing credentialSubject in claims");
        }
        return Result.success((Map<String, Object>) claims.get(CREDENTIAL_SUBJECT));
    }

    private TokenDecorator vcDecorator(String type, String participantId, VerifiableCredential credential) {
        var now = Date.from(clock.instant());
        return tp -> tp.claims(JwtRegisteredClaimNames.ISSUER, credential.getIssuer().id())
                .claims(JwtRegisteredClaimNames.ISSUED_AT, now)
                .claims(JwtRegisteredClaimNames.NOT_BEFORE, Date.from(credential.getIssuanceDate()))
                .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString())
                .claims(JwtRegisteredClaimNames.SUBJECT, participantId)
                .claims(VERIFIABLE_CREDENTIAL_CLAIM, createVcClaim(type, credential))
                .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Date.from(credential.getExpirationDate()));
    }

    private Map<String, Object> createVcClaim(String type, VerifiableCredential verifiableCredential) {
        return Map.of(
                JsonLdKeywords.CONTEXT, List.of(VcConstants.W3C_CREDENTIALS_URL),
                JsonLdKeywords.TYPE, List.of(VERIFIABLE_CREDENTIAL, type),
                CREDENTIAL_SUBJECT, credentialSubjectClaims(verifiableCredential));
    }

    private Map<String, Object> credentialSubjectClaims(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialSubject().size() == 1) {
            return verifiableCredential.getCredentialSubject().get(0).getClaims();
        } else {
            throw new UnsupportedOperationException("Only one credential subject is supported");
        }
    }

}
