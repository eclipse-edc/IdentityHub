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
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerSelfIssuedTokenVerifier;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DcpIssuerSelfIssuedTokenVerifierImplTest {

    public static final String ISSUER_DID = "did:web:issuer";
    public static final String PARTICIPANT_DID = "did:web:participant";
    public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
    public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);

    private final TokenValidationAction tokenValidationAction = mock();
    private final ParticipantStore participantStore = mock();
    private final DcpIssuerSelfIssuedTokenVerifier dcpIssuerTokenVerifier = new DcpIssuerSelfIssuedTokenVerifierImpl(participantStore, tokenValidationAction);


    @Test
    void verify() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(participantStore.query(any())).thenReturn(StoreResult.success(List.of(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationAction.apply(token)).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = dcpIssuerTokenVerifier.verify(token);

        assertThat(result).isSucceeded();
        Mockito.verify(participantStore).query(argThat(qs -> qs.getFilterExpression().stream().anyMatch(c -> c.getOperandRight().equals(PARTICIPANT_DID))));

    }

    @Test
    void verify_participantNotFound() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(participantStore.query(any())).thenReturn(StoreResult.success(List.of()));

        var result = dcpIssuerTokenVerifier.verify(token);

        assertThat(result).isFailed();

    }

    @Test
    void verify_tokenValidationFails() {

        var token = TokenRepresentation.Builder.newInstance().token(generateToken()).build();

        when(participantStore.query(any())).thenReturn(StoreResult.success(List.of(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, PARTICIPANT_DID))));
        when(tokenValidationAction.apply(token)).thenReturn(Result.failure("failed"));

        var result = dcpIssuerTokenVerifier.verify(token);

        assertThat(result).isFailed();

    }

    @Test
    void verify_faultyToken() {

        var token = TokenRepresentation.Builder.newInstance().token("faultyToken").build();

        var result = dcpIssuerTokenVerifier.verify(token);

        assertThat(result).isFailed();

    }
    
    private String generateToken() {
        return generateJwt(ISSUER_DID, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), PARTICIPANT_KEY);
    }
}
