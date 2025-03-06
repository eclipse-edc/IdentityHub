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

package org.eclipse.edc.issuerservice.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.StatusListInfoFactoryRegistryImpl;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListFactory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_CREDENTIAL;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_CREDENTIAL_JWT;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_REVOCATION_CREDENTIAL;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET;
import static org.eclipse.edc.issuerservice.credentials.statuslist.TestData.EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNEXPECTED;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CredentialStatusServiceImplTest {

    public static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {
    };
    private static final String REVOCATION_CREDENTIAL_ID = "https://example.com/credentials/status/3";
    private static final String CREDENTIAL_ID = "https://example.com/credentials/23894672394";
    private final ObjectMapper objectMapper = new JacksonTypeManager().getMapper().copy()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final CredentialStore credentialStore = mock();
    private CredentialStatusServiceImpl revocationService;
    private TokenGenerationService tokenGenerationService;
    private Monitor monitor;
    private ECKey signingKey;

    @BeforeEach
    void setUp() throws JOSEException {
        signingKey = new ECKeyGenerator(Curve.P_256).generate();
        tokenGenerationService = mock(TokenGenerationService.class);
        when(tokenGenerationService.generate(any(), any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("new-token").build()));
        monitor = mock();
        var reg = new StatusListInfoFactoryRegistryImpl();
        reg.register("BitstringStatusListEntry", new BitstringStatusListFactory(credentialStore));
        revocationService = new CredentialStatusServiceImpl(credentialStore, new NoopTransactionContext(), objectMapper,
                monitor, tokenGenerationService, () -> "some-private-key", reg, mock());
    }

    private SignedJWT sign(Map<String, Object> claims) {


        var jwsHeader = new JWSHeader(JWSAlgorithm.ES256);
        var claimsSet = new JWTClaimsSet.Builder();
        claims.forEach(claimsSet::claim);
        var signedJwt = new SignedJWT(jwsHeader, claimsSet.build());
        try {
            signedJwt.sign(new ECDSASigner(signingKey));
            return signedJwt;
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private VerifiableCredentialResource createCredential(String credentialJson, @Nullable String rawVc) {
        return createCredentialBuilder(credentialJson, rawVc)
                .build();
    }

    private VerifiableCredentialResource.Builder createCredentialBuilder(String credentialJson, @Nullable String rawVc) {
        try {
            var credential = objectMapper.readValue(credentialJson, VerifiableCredential.class);
            return VerifiableCredentialResource.Builder.newInstance()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(rawVc, CredentialFormat.VC1_0_JWT, credential))
                    .issuerId(credential.getIssuer().id())
                    .holderId("did:web:testholder");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Revoke {

        @Test
        void revokeCredential() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.update(any())).thenReturn(success());

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isSucceeded();
            verify(tokenGenerationService).generate(any(), any());
            verify(credentialStore, times(2)).update(any());
        }

        @Test
        void revokeCredential_credentialNotFound() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(notFound("foo"));
            when(credentialStore.update(any())).thenReturn(success());

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isFailed().detail().isEqualTo("foo");
            assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
        }

        @Test
        void revokeCredential_whenAlreadyRevoked() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.update(any())).thenReturn(success());

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isSucceeded();
            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
            verify(monitor).info(eq("Revocation not necessary, credential is already revoked."));
        }

        @Test
        void revokeCredential_noCredentialStatus() throws JsonProcessingException, ParseException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);
            claims.remove("credentialStatus");
            var jwt = sign(claims);


            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));


            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isFailed();
            assertThat(result.getFailure().getReason()).isEqualTo(BAD_REQUEST);
            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
        }

        @Test
        void revokeCredential_noRevocationCredentialUrl() throws JsonProcessingException, ParseException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);
            // remove statusListCredential, which is the revocation credential URL
            ((List<Map<String, Object>>) claims.get("credentialStatus")).get(0).remove("statusListCredential");
            var jwt = sign(claims);


            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isFailed()
                    .detail().containsSequence("is invalid, the 'statusListCredential' field is missing");
            assertThat(result.getFailure().getReason()).isEqualTo(UNEXPECTED);
            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
        }

        @Test
        void revokeCredential_noStatusIndex() throws ParseException, JsonProcessingException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);

            ((List<Map<String, Object>>) claims.get("credentialStatus")).get(0).remove("statusListIndex");
            var jwt = sign(claims);


            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isFailed()
                    .detail().containsSequence("is invalid, the 'statusListIndex' field is missing");
            assertThat(result.getFailure().getReason()).isEqualTo(UNEXPECTED);
            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
        }

        @Test
        void revokeCredential_noRevocationCredentialFound() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(notFound("foo"));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.update(any())).thenReturn(success());

            var result = revocationService.revokeCredential(CREDENTIAL_ID);
            assertThat(result).isFailed().detail().isEqualTo("foo");
            assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

            verifyNoInteractions(tokenGenerationService);
            verify(credentialStore, never()).update(any());
        }

    }

    @Nested
    class Suspend {
        @Test
        void suspend() {
            assertThatThrownBy(() -> revocationService.suspendCredential(CREDENTIAL_ID, null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Resume {
        @Test
        void resume() {
            assertThatThrownBy(() -> revocationService.resumeCredential(CREDENTIAL_ID, null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class GetCredentialStatus {
        @Test
        void getCredentialStatus() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isSucceeded().isNull();
            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_whenRevoked() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredentialBuilder(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))
                            .state(VcStatus.REVOKED)
                            .build()));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isSucceeded().isEqualTo("revocation");
            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_credentialNotFound() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(notFound("foo"));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isFailed().detail().isEqualTo("foo");
            assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_noRevocationCredentialFound() {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID))).thenReturn(notFound("foo"));
            when(credentialStore.findById(eq(CREDENTIAL_ID))).thenReturn(success(createCredential(EXAMPLE_CREDENTIAL, EXAMPLE_CREDENTIAL_JWT.replace("\n", ""))));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isFailed().detail().isEqualTo("foo");
            assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_noCredentialStatus() throws ParseException, JsonProcessingException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);
            claims.remove("credentialStatus");
            var jwt = sign(claims);

            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));


            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isFailed();
            assertThat(result.getFailure().getReason()).isEqualTo(BAD_REQUEST);
            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_noCredentialUrl() throws ParseException, JsonProcessingException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);
            // remove statusListCredential, which is the revocation credential URL
            ((List<Map<String, Object>>) claims.get("credentialStatus")).get(0).remove("statusListCredential");
            var jwt = sign(claims);


            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isFailed()
                    .detail().containsSequence("is invalid, the 'statusListCredential' field is missing");
            assertThat(result.getFailure().getReason()).isEqualTo(UNEXPECTED);
            verifyNoInteractions(tokenGenerationService);
        }

        @Test
        void getCredentialStatus_noStatusIndex() throws ParseException, JsonProcessingException {
            when(credentialStore.findById(eq(REVOCATION_CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT.replace("\n", ""))));


            var claims = objectMapper.readValue(EXAMPLE_CREDENTIAL, MAP_REF);

            ((List<Map<String, Object>>) claims.get("credentialStatus")).get(0).remove("statusListIndex");
            var jwt = sign(claims);


            when(credentialStore.findById(eq(CREDENTIAL_ID)))
                    .thenReturn(success(createCredential(objectMapper.writeValueAsString(jwt.getJWTClaimsSet().getClaims()), jwt.serialize())));

            var result = revocationService.getCredentialStatus(CREDENTIAL_ID);
            assertThat(result).isFailed()
                    .detail().containsSequence("is invalid, the 'statusListIndex' field is missing");
            assertThat(result.getFailure().getReason()).isEqualTo(UNEXPECTED);
            verifyNoInteractions(tokenGenerationService);
        }

    }

}