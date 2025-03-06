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

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
import static org.mockito.Mockito.when;

public class DcpHolderTokenVerifierImplTest {

    public static final String ISSUER_DID = "did:web:issuer";
    public static final String PARTICIPANT_DID = "did:web:participant";
    public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
    public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);

    private final TokenValidationRulesRegistry rulesRegistry = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final PublicKeyResolver publicKeyResolver = mock();

    private final HolderStore holderStore = mock();
    private final DcpHolderTokenVerifier dcpIssuerTokenVerifier = new DcpHolderTokenVerifierImpl(rulesRegistry, tokenValidationService, publicKeyResolver, holderStore);

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance().participantContextId("holderId")
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

    private Holder createHolder(String id, String did, String name) {
        return Holder.Builder.newInstance()
                .participantContextId(UUID.randomUUID().toString())
                .holderId(id)
                .did(did)
                .holderName(name)
                .build();
    }


    private String generateToken() {
        return generateJwt(ISSUER_DID, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), PARTICIPANT_KEY);
    }
}
