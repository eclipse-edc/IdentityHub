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
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator.VERIFIABLE_CREDENTIAL_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtCredentialGeneratorTest {

    public static final String PRIVATE_KEY_ALIAS = "private-key";
    public static final String PUBLIC_KEY_ID = "key-1";

    public static final List<String> REQUIRED_CLAIMS = asList("exp", "iss", "nbf", "jti", "sub", VERIFIABLE_CREDENTIAL_CLAIM);
    private final JwsSignerProvider signerProvider = mock();
    private final TokenGenerationService tokenGenerationService = new JwtGenerationService(signerProvider);
    private final JwtCredentialGenerator jwtCredentialGenerator = new JwtCredentialGenerator(tokenGenerationService, Clock.systemUTC());

    @BeforeEach
    void setup() throws JOSEException {
        var vpSigningKey = createKey(Curve.P_384, "vc-key");
        when(signerProvider.createJwsSigner(anyString())).thenReturn(Result.failure("not found"));
        when(signerProvider.createJwsSigner(eq(PRIVATE_KEY_ALIAS))).thenReturn(Result.success(new ECDSASigner(vpSigningKey)));

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void generateCredential() {

        var subjectClaims = Map.of("name", "Foo Bar");
        var statusClaims = Map.of("id", UUID.randomUUID().toString(),
                "type", "BitStringStatusListEntry",
                "statusPurpose", "revocation",
                "statusListIndex", 42,
                "statusCredential", "https://foo.bar/baz.json");
        Map<String, Object> claims = Map.of("credentialSubject", subjectClaims, "credentialStatus", statusClaims);

        var result = jwtCredentialGenerator.generateCredential(createCredentialDefinition(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

        assertThat(result).isSucceeded();

        var container = result.getContent();
        assertThat(container.rawVc()).isNotNull();
        assertThat(container.format()).isEqualTo(VC1_0_JWT);
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
        assertThat(extractedClaims.getClaim(VERIFIABLE_CREDENTIAL_CLAIM)).isInstanceOfSatisfying(Map.class, vcClaim -> {
            assertThat((List) vcClaim.get("type")).contains("MembershipCredential");
            assertThat((Map) vcClaim.get("credentialSubject")).containsAllEntriesOf(subjectClaims);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void generateCredential_whenNoStatus() {

        var subjectClaims = Map.of("name", "Foo Bar");
        Map<String, Object> claims = Map.of("credentialSubject", subjectClaims);

        var result = jwtCredentialGenerator.generateCredential(createCredentialDefinition(), PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

        assertThat(result).isSucceeded();

        var container = result.getContent();
        assertThat(container.rawVc()).isNotNull();
        assertThat(container.format()).isEqualTo(VC1_0_JWT);
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

        REQUIRED_CLAIMS.forEach(claim -> assertThat(extractedClaims.getClaim(claim)).describedAs("Claim '%s' cannot be null", claim).isNotNull());
        assertThat(extractJwtHeader(container.rawVc()).getKeyID()).isEqualTo("did:example:issuer#%s".formatted(PUBLIC_KEY_ID));
        assertThat(extractedClaims.getClaim(VERIFIABLE_CREDENTIAL_CLAIM)).isInstanceOfSatisfying(Map.class, vcClaim -> {
            assertThat((List) vcClaim.get("type")).contains("MembershipCredential");
            assertThat((Map) vcClaim.get("credentialSubject")).containsAllEntriesOf(subjectClaims);
        });
    }

    @Test
    void generateCredential_fails_whenPrivateKeyNotFound() {

        var subjectClaims = Map.of("name", "Foo Bar");
        Map<String, Object> claims = Map.of("credentialSubject", subjectClaims);

        var result = jwtCredentialGenerator.generateCredential(createCredentialDefinition(), "not_found", PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

        assertThat(result).isFailed();


    }

    @Test
    void generateCredential_fails_whenCredentialSubjectNotFoundInClaims() {

        Map<String, Object> claims = Map.of();

        var result = jwtCredentialGenerator.generateCredential(createCredentialDefinition(), "not_found", PUBLIC_KEY_ID, "did:example:issuer", "did:example:participant", claims);

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
        var res = jwtCredentialGenerator.signCredential(credential, PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID);

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
        var res = jwtCredentialGenerator.signCredential(credential, PRIVATE_KEY_ALIAS, PUBLIC_KEY_ID);

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
        var res = jwtCredentialGenerator.signCredential(credential, "non-exist", PUBLIC_KEY_ID);

        assertThat(res).isFailed().detail().contains("not found");
    }

    protected ECKey createKey(Curve p256, String centralIssuerKeyId) {
        try {
            return new ECKeyGenerator(p256)
                    .keyID(centralIssuerKeyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private CredentialDefinition createCredentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .credentialType("MembershipCredential")
                .mapping(new MappingDefinition("input", "outut", true))
                .jsonSchema("{}")
                .participantContextId(UUID.randomUUID().toString())
                .formatFrom(VC1_0_JWT)
                .build();
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
}
