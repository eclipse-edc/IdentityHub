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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus.CredentialRequestStatusApiController;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
@SuppressWarnings("resource")
class CredentialRequestStatusApiControllerTest extends RestControllerTestBase {

    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final IssuanceProcessService issuerService = mock();
    private final DcpHolderTokenVerifier dcpIssuerTokenVerifier = mock();
    private final ParticipantContextService participantContextService = mock();
    private final String participantContextId = "participantContextId";
    private final String participantId = "holderId";
    private final String participantContextIdEncoded = Base64.getEncoder().encodeToString(participantContextId.getBytes());

    @Test
    void credentialStatus_tokenNotPresent_shouldReturn401() {
        assertThatThrownBy(() -> controller().credentialStatus(participantContextIdEncoded, participantId, null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authorization header missing");

        verifyNoInteractions(issuerService, dcpIssuerTokenVerifier, typeTransformerRegistry);

    }

    @Test
    void credentialStatus_transformationError_shouldReturn400() {

        var participant = new Holder("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(issuerService.search(any())).thenReturn(ServiceResult.success(List.of(createIssuanceProcess())));

        when(typeTransformerRegistry.transform(isA(CredentialRequestStatus.class), eq(JsonObject.class))).thenReturn(Result.failure("cannot transform"));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));
        assertThatThrownBy(() -> controller().credentialStatus(participantContextIdEncoded, participantId, generateJwt()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("cannot transform");

    }

    @Test
    void credentialStatus_tokenVerificationFails_shouldReturn401() {
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));

        assertThatThrownBy(() -> controller().credentialStatus(participantContextIdEncoded, participantId, generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("unauthorized");

        verifyNoInteractions(issuerService);
    }

    @Test
    void credentialStatus_participantNotFound_shouldReturn401() {
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller().credentialStatus(participantContextIdEncoded, participantId, generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid issuer");

        verifyNoInteractions(issuerService);
    }


    @Test
    void credentialStatus() {

        var participant = new Holder("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());

        var token = generateJwt();

        when(issuerService.search(any())).thenReturn(ServiceResult.success(List.of(createIssuanceProcess())));
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));
        when(typeTransformerRegistry.transform(isA(CredentialRequestStatus.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));

        var response = controller().credentialStatus(participantContextIdEncoded, participantId, token);

        assertThat(response).isNotNull();

        verify(dcpIssuerTokenVerifier).verify(any(), argThat(tr -> tr.getToken().equals(token)));
    }

    @Override
    protected CredentialRequestStatusApiController controller() {
        return new CredentialRequestStatusApiController(participantContextService, dcpIssuerTokenVerifier, issuerService, typeTransformerRegistry);
    }


    private ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(participantContextId)
                .did("did")
                .apiTokenAlias("apiTokenAlias")
                .build();
    }

    private IssuanceProcess createIssuanceProcess() {
        return IssuanceProcess.Builder.newInstance()
                .memberId(participantId)
                .participantContextId(participantContextId)
                .holderPid(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .state(IssuanceProcessStates.DELIVERED.code())
                .build();
    }

    private String generateJwt() {
        var ecKey = generateEcKey(null);
        var jwt = buildSignedJwt(new JWTClaimsSet.Builder().audience("test-audience")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issuer("test-issuer")
                .subject("test-subject")
                .jwtID(UUID.randomUUID().toString()).build(), ecKey);

        return jwt.serialize();
    }

}