/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.issuance.generator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.CREDENTIAL_STATUS;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.CREDENTIAL_SUBJECT;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.ID;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.ISSUER;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.TYPE;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.VALID_FROM;
import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGeneratorTest.PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGeneratorTest.PUBLIC_KEY_ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JoseVcdm20CredentialGeneratorTest {
    public static final List<String> REQUIRED_CLAIMS = asList("@context", CREDENTIAL_SUBJECT, ID, VALID_FROM, ISSUER, TYPE);
    private static final String TEST_PARTICIPANT = "test-participant";
    private final JwsSignerProvider signerProvider = mock();
    private final TokenGenerationService tokenGenerationService = new JwtGenerationService(signerProvider);
    private final JoseVcdm20CredentialGenerator jwtCredentialGenerator = new JoseVcdm20CredentialGenerator(tokenGenerationService, Clock.systemUTC());

    @BeforeEach
    void setup() throws JOSEException {
        var vpSigningKey = createKey(Curve.P_384, "vc-key");
        when(signerProvider.createJwsSigner(eq(TEST_PARTICIPANT), anyString())).thenReturn(Result.failure("not found"));
        when(signerProvider.createJwsSigner(eq(TEST_PARTICIPANT), eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(new ECDSASigner(vpSigningKey)));

        when(signerProvider.createJwsSigner(anyString())).thenThrow(new AssertionError("This method is deprecated and not to be used anymore"));
    }

    @Test
    void generateCredential() throws ParseException {
        var subjectClaims = Map.of("name", "Foo Bar");
        var statusClaims = Map.of("id", UUID.randomUUID().toString(),
                TYPE, "BitStringStatusListEntry",
                "statusPurpose", "revocation",
                "statusListIndex", 42,
                "statusCredential", "https://foo.bar/baz.json");
        Map<String, Object> claims = Map.of(CREDENTIAL_SUBJECT, subjectClaims, CREDENTIAL_STATUS, statusClaims);

        var result = jwtCredentialGenerator.generateCredential(TEST_PARTICIPANT, createCredentialDefinition(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

        assertThat(result).isSucceeded();

        var container = result.getContent();
        assertThat(container.rawVc()).isNotNull();
        assertThat(container.format()).isEqualTo(VC2_0_JOSE);
        assertThat(container.credential()).satisfies(verifiableCredential -> {
            assertThat(verifiableCredential.getType()).contains("MembershipCredential");
            assertThat(verifiableCredential.getIssuer().id()).isEqualTo("did:example:issuer");
            assertThat(verifiableCredential.getCredentialSubject()).hasSize(1);

            var subject = verifiableCredential.getCredentialSubject().get(0);

            assertThat(subject.getId()).isEqualTo("did:example:participant");
            assertThat(subject.getClaims()).isEqualTo(subjectClaims);

            assertThat(verifiableCredential.getCredentialStatus()).isNotEmpty();
        });


        var extractedClaims = extractJwtClaims(container.rawVc());

        REQUIRED_CLAIMS.forEach(claim -> assertThat(extractedClaims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
        assertThat(extractJwtHeader(container.rawVc()).getKeyID()).isEqualTo("did:example:issuer#%s".formatted(PUBLIC_KEY_ID));

        assertThat(extractedClaims.getStringArrayClaim(TYPE)).contains("MembershipCredential");
        assertThat(extractedClaims.getClaim(CREDENTIAL_STATUS)).isInstanceOf(Map.class);
        assertThat(extractedClaims.getClaim(CREDENTIAL_SUBJECT)).isInstanceOfSatisfying(Map.class, subjectClaimsSet -> {
            //noinspection unchecked
            assertThat(subjectClaimsSet).containsAllEntriesOf(subjectClaims);
        });
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void generateCredential_whenNoStatus() throws ParseException {

        var subjectClaims = Map.of("name", "Foo Bar");
        Map<String, Object> claims = Map.of(CREDENTIAL_SUBJECT, subjectClaims);

        var result = jwtCredentialGenerator.generateCredential(TEST_PARTICIPANT, createCredentialDefinition(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

        assertThat(result).isSucceeded();

        var container = result.getContent();
        assertThat(container.rawVc()).isNotNull();
        assertThat(container.format()).isEqualTo(VC2_0_JOSE);
        assertThat(container.credential()).satisfies(verifiableCredential -> {
            assertThat(verifiableCredential.getType()).contains("MembershipCredential");
            assertThat(verifiableCredential.getIssuer().id()).isEqualTo("did:example:issuer");
            assertThat(verifiableCredential.getCredentialSubject()).hasSize(1);

            var subject = verifiableCredential.getCredentialSubject().get(0);

            assertThat(subject.getId()).isEqualTo("did:example:participant");
            assertThat(subject.getClaims()).isEqualTo(subjectClaims);

            assertThat(verifiableCredential.getCredentialStatus()).isEmpty();
        });


        var extractedClaims = extractJwtClaims(container.rawVc());

        assertThat(extractJwtHeader(container.rawVc()).getKeyID()).isEqualTo("did:example:issuer#%s".formatted(PUBLIC_KEY_ID));
        REQUIRED_CLAIMS.forEach(claim -> assertThat(extractedClaims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
        assertThat(extractJwtHeader(container.rawVc()).getKeyID()).isEqualTo("did:example:issuer#%s".formatted(PUBLIC_KEY_ID));
        assertThat(extractedClaims.getClaims()).doesNotContainKey(CREDENTIAL_STATUS);
    }

    @Test
    void generateCredential_fails_whenPrivateKeyNotFound() {
        var subjectClaims = Map.of("name", "Foo Bar");
        Map<String, Object> claims = Map.of(CREDENTIAL_SUBJECT, subjectClaims);

        var result = jwtCredentialGenerator.generateCredential(TEST_PARTICIPANT, createCredentialDefinition(), "not_found", PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);
        assertThat(result).isFailed();
    }

    @Test
    void generateCredential_fails_whenCredentialSubjectNotFoundInClaims() {
        Map<String, Object> claims = Map.of();

        var result = jwtCredentialGenerator.generateCredential(TEST_PARTICIPANT, createCredentialDefinition(), "not_found", PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);
        assertThat(result).isFailed().detail().contains("Missing credentialSubject in claims");
    }

    @Test
    void signCredential() {
        var now = Instant.now();
        var credential = VerifiableCredential.Builder.newInstance()
                .type("TestCredential")
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(now)
                .expirationDate(now.plusSeconds(3600))
                .credentialStatus(new CredentialStatus("status-id", "StatusTypeEntry", Map.of("key", "value")))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("foo", "bar")
                        .build())
                .build();
        var res = jwtCredentialGenerator.signCredential(TEST_PARTICIPANT, credential, PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID);

        assertThat(res).isSucceeded().satisfies(jwt -> {
            assertThatNoException().isThrownBy(() -> SignedJWT.parse(jwt));
            assertThat(SignedJWT.parse(jwt).getHeader().getKeyID()).endsWith(PUBLIC_KEY_ID);
        });
    }

    @Test
    void signCredential_whenNoStatus() {
        var now = Instant.now();
        var credential = VerifiableCredential.Builder.newInstance()
                .type("TestCredential")
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(now)
                .expirationDate(now.plusSeconds(3600))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("foo", "bar")
                        .build())
                .build();
        var res = jwtCredentialGenerator.signCredential(TEST_PARTICIPANT, credential, PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID);

        assertThat(res).isSucceeded().satisfies(jwt -> {
            assertThatNoException().isThrownBy(() -> SignedJWT.parse(jwt));
            assertThat(SignedJWT.parse(jwt).getHeader().getKeyID()).endsWith(PUBLIC_KEY_ID);
        });
    }

    @Test
    void signCredential_whenPrivateKeyNotFound() {
        var now = Instant.now();
        var credential = VerifiableCredential.Builder.newInstance()
                .type("TestCredential")
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(now)
                .expirationDate(now.plusSeconds(3600))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("foo", "bar")
                        .build())
                .build();
        var res = jwtCredentialGenerator.signCredential(TEST_PARTICIPANT, credential, "non-exist", PUBLIC_KEY_ID);

        assertThat(res).isFailed().detail().contains("not found");
    }

    private ECKey createKey(Curve curve, String centralIssuerKeyId) {
        try {
            return new ECKeyGenerator(curve)
                    .keyID(centralIssuerKeyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private JWTClaimsSet extractJwtClaims(String vpJwt) {
        try {
            return SignedJWT.parse(vpJwt).getJWTClaimsSet();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private JWSHeader extractJwtHeader(String vpJwt) {
        try {
            return SignedJWT.parse(vpJwt).getHeader();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private CredentialDefinition createCredentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .credentialType("MembershipCredential")
                .mapping(new MappingDefinition("input", "output", true))
                .jsonSchema("{}")
                .participantContextId(UUID.randomUUID().toString())
                .formatFrom(VC2_0_JOSE)
                .build();
    }
}