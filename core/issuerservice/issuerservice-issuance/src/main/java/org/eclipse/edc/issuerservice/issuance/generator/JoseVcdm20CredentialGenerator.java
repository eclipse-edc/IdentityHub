/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator.CREDENTIAL_STATUS;
import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator.CREDENTIAL_SUBJECT;
import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator.VERIFIABLE_CREDENTIAL;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class JoseVcdm20CredentialGenerator implements CredentialGenerator {

    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;

    public JoseVcdm20CredentialGenerator(TokenGenerationService tokenGenerationService, Clock clock) {
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(String participantContextId, CredentialDefinition definition,
                                                                    String privateKeyAlias, String publicKeyId, String issuerId,
                                                                    String holderDid, Map<String, Object> claims) {

        var subjectResult = extractCredentialSubject(claims);
        if (subjectResult.failed()) {
            return subjectResult.mapFailure();
        }

        var statusResult = createCredentialStatus(claims);

        //noinspection unchecked
        var builder = VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(issuerId))
                .dataModelVersion(DataModelVersion.V_2_0)
                .issuanceDate(Instant.now(clock))
                .expirationDate(Instant.now(clock).plusSeconds(definition.getValidity()))
                .types(List.of(VERIFIABLE_CREDENTIAL, definition.getCredentialType()))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(holderDid)
                        .claims(subjectResult.getContent())
                        .build());

        statusResult.onSuccess(builder::credentialStatus);

        var vc = builder.build();

        return signCredential(participantContextId, vc, privateKeyAlias, publicKeyId)
                .map(token -> new VerifiableCredentialContainer(token, CredentialFormat.VC2_0_JOSE, vc));
    }

    @Override
    public Result<String> signCredential(String participantContextId, VerifiableCredential credential, String privateKeyAlias,
                                         String publicKeyId) {

        var issuerId = credential.getIssuer().id();
        var holderDid = credential.getCredentialSubject().iterator().next().getId();

        var composedKeyId = publicKeyId.startsWith(issuerId)
                ? publicKeyId
                : issuerId + "#" + publicKeyId;

        TokenDecorator decorator = tokenBuilder -> {

            var vcClaim = createVcClaim(credential);

            tokenBuilder
                    .claims(ISSUER, issuerId)
                    .claims(SUBJECT, holderDid)
                    .claims(ISSUED_AT, Date.from(clock.instant()))
                    .claims(NOT_BEFORE, Date.from(credential.getIssuanceDate()))
                    .claims(JWT_ID, UUID.randomUUID().toString())
                    .claims("vc", vcClaim);

            if (credential.getExpirationDate() != null) {
                tokenBuilder.claims(EXPIRATION_TIME, Date.from(credential.getExpirationDate()));
            }

            return tokenBuilder;
        };

        return tokenGenerationService
                .generate(participantContextId, privateKeyAlias, decorator, new KeyIdDecorator(composedKeyId))
                .map(TokenRepresentation::getToken);
    }

    private Map<String, Object> createVcClaim(VerifiableCredential vc) {

        return new HashMap<>(Map.of(
                JsonLdKeywords.CONTEXT, List.of("https://www.w3.org/ns/credentials/v2"),
                "id", vc.getId(),
                "type", vc.getType(),
                "issuer", vc.getIssuer().id(),
                "issuanceDate", vc.getIssuanceDate().toString(),
                "credentialSubject", credentialSubjectClaims(vc)
        ));
    }

    private Map<String, Object> credentialSubjectClaims(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialSubject().size() == 1) {
            return verifiableCredential.getCredentialSubject().get(0).getClaims();
        } else {
            throw new UnsupportedOperationException("Only one credential subject is supported");
        }
    }

    @SuppressWarnings("unchecked")
    private Result<Map<String, Object>> extractCredentialSubject(Map<String, Object> claims) {
        if (!claims.containsKey(CREDENTIAL_SUBJECT)) {
            return Result.failure("Missing credentialSubject in claims");
        }
        return Result.success((Map<String, Object>) claims.get(CREDENTIAL_SUBJECT));
    }

    @SuppressWarnings("unchecked")
    private Result<CredentialStatus> createCredentialStatus(Map<String, Object> claims) {
        if (!claims.containsKey(CREDENTIAL_STATUS)) {
            return Result.failure("no credentialStatus in claims");
        }
        var statusClaims = (Map<String, Object>) claims.get(CREDENTIAL_STATUS);

        return Result.success(new CredentialStatus((String) statusClaims.get("id"),
                (String) statusClaims.get("type"),
                statusClaims));
    }
}