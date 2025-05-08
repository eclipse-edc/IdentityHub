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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtCredentialGenerator implements CredentialGenerator {

    public static final String VERIFIABLE_CREDENTIAL_CLAIM = "vc";
    public static final String CREDENTIAL_SUBJECT = "credentialSubject";
    public static final String CREDENTIAL_STATUS = "credentialStatus";
    public static final String VERIFIABLE_CREDENTIAL = "VerifiableCredential";
    public static final String TYPE_PROPERTY = "type";
    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;

    public JwtCredentialGenerator(TokenGenerationService tokenGenerationService, Clock clock) {
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(CredentialDefinition definition, String privateKeyAlias, String publicKeyId, String issuerId, String holderDid, Map<String, Object> claims) {

        var subjectResult = getCredentialSubject(claims);
        if (subjectResult.failed()) {
            return subjectResult.mapFailure();
        }

        var statusResult = createCredentialStatus(claims);

        var credentialBuilder = generateVerifiableCredential(definition.getCredentialType(), definition.getValidity(), issuerId, holderDid, subjectResult.getContent());

        statusResult.onSuccess(credentialBuilder::credentialStatus);

        var credential = credentialBuilder.build();
        return signCredentialInternal(credential, privateKeyAlias, publicKeyId, issuerId, holderDid, VERIFIABLE_CREDENTIAL, definition.getCredentialType())
                .map(token -> new VerifiableCredentialContainer(token, CredentialFormat.VC1_0_JWT, credential));
    }

    @Override
    public Result<String> signCredential(VerifiableCredential credential, String privateKeyAlias, String publicKeyId) {
        var issuerId = credential.getIssuer().id();
        var type = credential.getType().toArray(new String[0]);
        var holderDid = credential.getCredentialSubject().iterator().next().getId();

        return signCredentialInternal(credential, privateKeyAlias, publicKeyId, issuerId, holderDid, type);
    }


    private Result<String> signCredentialInternal(VerifiableCredential credential, String privateKeyAlias, String publicKeyId, String issuerId, String holderDid, String... types) {
        var composedKeyId = publicKeyId;
        if (!publicKeyId.startsWith(issuerId)) {
            composedKeyId = issuerId + "#" + publicKeyId;
        }

        return tokenGenerationService.generate(privateKeyAlias, vcDecorator(holderDid, credential, types), new KeyIdDecorator(composedKeyId))
                .map(TokenRepresentation::getToken);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private VerifiableCredential.Builder generateVerifiableCredential(String type, long validity, String issuer, String holderId, Map<String, Object> credentialSubject) {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(issuer))
                .dataModelVersion(DataModelVersion.V_1_1)
                .issuanceDate(Instant.now(clock))
                .expirationDate(Instant.now(clock).plusSeconds(validity))
                .types(List.of(VERIFIABLE_CREDENTIAL, type))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(holderId)
                        .claims(credentialSubject)
                        .build());
    }

    @SuppressWarnings("unchecked")
    private Result<Map<String, Object>> getCredentialSubject(Map<String, Object> claims) {
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

    private TokenDecorator vcDecorator(String participantId, VerifiableCredential credential, String... type) {
        var now = Date.from(clock.instant());
        return tp -> tp.claims(JwtRegisteredClaimNames.ISSUER, credential.getIssuer().id())
                .claims(JwtRegisteredClaimNames.ISSUED_AT, now)
                .claims(JwtRegisteredClaimNames.NOT_BEFORE, Date.from(credential.getIssuanceDate()))
                .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString())
                .claims(JwtRegisteredClaimNames.SUBJECT, participantId)
                .claims(VERIFIABLE_CREDENTIAL_CLAIM, createVcClaim(credential, type))
                .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Date.from(credential.getExpirationDate())); //todo: this will fail for credentials that don't have an expiration date
    }

    private Map<String, Object> createVcClaim(VerifiableCredential verifiableCredential, String... type) {
        var claims = new HashMap<>(
                Map.of(JsonLdKeywords.CONTEXT, List.of(VcConstants.W3C_CREDENTIALS_URL),
                        TYPE_PROPERTY, Arrays.asList(type),
                        "id", verifiableCredential.getId(),
                        "issuanceDate", verifiableCredential.getIssuanceDate().toString(),
                        "issuer", verifiableCredential.getIssuer().id(),
                        CREDENTIAL_SUBJECT, credentialSubjectClaims(verifiableCredential)
                ));
        if (verifiableCredential.getExpirationDate() != null) {
            claims.put("expirationDate", verifiableCredential.getExpirationDate().toString());
        }
        if (verifiableCredential.getDescription() != null) {
            claims.put("description", verifiableCredential.getDescription());
        }
        if (verifiableCredential.getName() != null) {
            claims.put("name", verifiableCredential.getName());
        }

        var status = credentialStatusClaims(verifiableCredential);
        if (!status.isEmpty()) {
            claims.put(CREDENTIAL_STATUS, credentialStatusClaims(verifiableCredential));
        }
        return claims;
    }

    private Map<String, Object> credentialStatusClaims(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialStatus().isEmpty()) {
            return Map.of();
        }
        var status = verifiableCredential.getCredentialStatus().get(0);
        var statusMap = new HashMap<String, Object>(Map.of("id", status.id(),
                "type", status.type()));
        statusMap.putAll(status.additionalProperties());
        return statusMap;
    }

    private Map<String, Object> credentialSubjectClaims(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialSubject().size() == 1) {
            return verifiableCredential.getCredentialSubject().get(0).getClaims();
        } else {
            throw new UnsupportedOperationException("Only one credential subject is supported");
        }
    }

}
