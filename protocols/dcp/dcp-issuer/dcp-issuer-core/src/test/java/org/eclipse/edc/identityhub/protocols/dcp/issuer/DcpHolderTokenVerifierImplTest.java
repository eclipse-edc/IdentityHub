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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DcpHolderTokenVerifierImplTest {

    public static final String ISSUER_DID = "did:web:issuer";
    public static final String PARTICIPANT_DID = "did:web:participant";
    private static final String ISSUER_PARTICIPANT_CONTEXT_ID = "holderId";
    public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
    public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);

    private final TokenValidationRulesRegistry rulesRegistry = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final PublicKeyResolver publicKeyResolver = mock();

    private final HolderStore holderStore = mock();
    private final DcpHolderTokenVerifier dcpIssuerTokenVerifier = new DcpHolderTokenVerifierImpl(rulesRegistry, tokenValidationService, publicKeyResolver, holderStore, false);

    private final IdentityHubParticipantContext participantContext = IdentityHubParticipantContext.Builder.newInstance().participantContextId(ISSUER_PARTICIPANT_CONTEXT_ID)
            .did(PARTICIPANT_DID)
            .apiTokenAlias("apiAlias")
            .build();

    @Test
    void verify() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isSucceeded();
        Mockito.verify(holderStore).query(argThat(qs -> qs.getFilterExpression().stream().anyMatch(c -> c.getOperandRight().equals(PARTICIPANT_DID))));

    }

    @Test
    @DisplayName("B5.4: an access token supplied by the Holder is carried on the request context")
    void verify_shouldCaptureAccessToken() {
        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("token", "holder-access-token").build()));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isSucceeded()
                .satisfies(context -> org.assertj.core.api.Assertions.assertThat(context.accessToken()).isEqualTo("holder-access-token"));
    }

    @Test
    @DisplayName("B5.4: the request context carries no access token when the Holder did not supply one")
    void verify_whenNoAccessToken_shouldBeNull() {
        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isSucceeded()
                .satisfies(context -> org.assertj.core.api.Assertions.assertThat(context.accessToken()).isNull());
    }

    @Test
    void verify_participantNotFound() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of()));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isFailed();

    }

    @Test
    void verify_tokenValidationFails() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.failure("failed"));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isFailed();

    }

    @Test
    void verify_faultyToken() {

        var token = TokenRepresentation.Builder.newInstance().token("faultyToken").build();

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isFailed();

    }

    // B2.6: token without a 'kid' JOSE header -> unauthorized
    @DisplayName("B2.6: a token without a 'kid' JOSE header is rejected as unauthorized")
    @Test
    void verify_missingKidHeader_returnsUnauthorized() throws JOSEException {
        // signed JWT whose JOSE header does NOT carry a 'kid' (the generateJwt() fixture always sets one)
        var claims = new JWTClaimsSet.Builder()
                .audience(ISSUER_DID)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
        jwt.sign(new ECDSASigner(PARTICIPANT_KEY.toECPrivateKey()));
        var token = TokenRepresentation.Builder.newInstance().token(jwt.serialize()).build();

        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        assertThat(result).isFailed().detail().isEqualTo("Kid not present");
        verifyNoInteractions(tokenValidationService);
    }

    // B2.10: holder exists but is registered under a DIFFERENT participant context than the issuer context being addressed -> must be rejected
    @DisplayName("B2.10: a holder registered under a different participant context must not authenticate against this issuer context")
    @Test
    void verify_holderRegisteredUnderDifferentParticipantContext_shouldBeRejected() {
        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        // holder is registered under a different issuer participant context than participantContext ("holderId")
        var foreignHolder = Holder.Builder.newInstance()
                .participantContextId("other-issuer-context")
                .holderId("foreignHolderId")
                .did(PARTICIPANT_DID)
                .holderName("foreign holder")
                .build();
        when(holderStore.query(any())).thenReturn(StoreResult.success(List.of(foreignHolder)));
        when(tokenValidationService.validate(anyString(), any(), anyList())).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = dcpIssuerTokenVerifier.verify(participantContext, token);

        // NOTE: currently this SUCCEEDS - the HolderStore query filters only on 'did', not on participantContextId,
        //  so a holder of issuer context A can authenticate against issuer context B (cross-tenant authentication).
        //  Once scoping is implemented, the query must also filter on the addressed issuer's participantContextId.
        assertThat(result).isFailed();
    }

    private Holder createHolder(String id, String did, String name) {
        return Holder.Builder.newInstance()
                .participantContextId(ISSUER_PARTICIPANT_CONTEXT_ID)
                .holderId(id)
                .did(did)
                .holderName(name)
                .build();
    }


    private String generateToken() {
        return generateJwt(ISSUER_DID, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), PARTICIPANT_KEY);
    }
}
