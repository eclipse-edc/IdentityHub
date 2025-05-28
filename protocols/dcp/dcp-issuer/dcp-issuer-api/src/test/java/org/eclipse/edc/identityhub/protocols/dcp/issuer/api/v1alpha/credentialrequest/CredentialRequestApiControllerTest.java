/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;
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
class CredentialRequestApiControllerTest extends RestControllerTestBase {

    private final JsonObjectValidatorRegistry validatorRegistryMock = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DcpIssuerService dcpIssuerService = mock();
    private final DcpHolderTokenVerifier dcpIssuerTokenVerifier = mock();
    private final JsonLdNamespace namespace = DSPACE_DCP_NAMESPACE_V_1_0;
    private final ParticipantContextService participantContextService = mock();
    private final String participantContextId = "participantContextId";
    private final String participantContextIdEncoded = Base64.getEncoder().encodeToString(participantContextId.getBytes());

    @Test
    void requestCredential_tokenNotPresent_shouldReturn401() {
        assertThatThrownBy(() -> controller().requestCredential(participantContextId, createObjectBuilder().build(), null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authorization header missing");

        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier, typeTransformerRegistry);

    }

    @Test
    void requestCredential_validationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(failure(violation("foo", "bar")));

        assertThatThrownBy(() -> controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + generateJwt()))
                .isInstanceOf(ValidationFailureException.class)
                .hasMessage("foo");
        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier, typeTransformerRegistry);

    }

    @Test
    void requestCredential_transformationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.failure("cannot transform"));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));
        assertThatThrownBy(() -> controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + generateJwt()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("cannot transform");

        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier);
    }

    @Test
    void requestCredential_tokenVerificationFails_shouldReturn401() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));

        assertThatThrownBy(() -> controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("unauthorized");

        verifyNoInteractions(dcpIssuerService);
    }

    @Test
    void requestCredential_participantNotFound_shouldReturn401() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid issuer");

        verifyNoInteractions(dcpIssuerService);
    }

    @Test
    void requestCredential_initiateCredentialIssuanceFails_shouldReturn_401() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        var participant = createHolder("id", "did", "name");

        var ctx = new DcpRequestContext(participant, Map.of());
        var token = generateJwt();
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(dcpIssuerService.initiateCredentialsIssuance(eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("cannot initiate unauthorized"));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));

        assertThatThrownBy(() -> controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + token))
                .isExactlyInstanceOf(NotAuthorizedException.class)
                .hasMessage("cannot initiate unauthorized");

        verify(dcpIssuerTokenVerifier).verify(any(), argThat(tr -> tr.getToken().equals(token)));
        verify(dcpIssuerService).initiateCredentialsIssuance(participantContextId, requestMessage, ctx);
    }

    @Test
    void requestCredential() {

        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        var participant = createHolder("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());

        var token = generateJwt();
        var responseMessage = new CredentialRequestMessage.Response(UUID.randomUUID().toString());
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(dcpIssuerService.initiateCredentialsIssuance(eq(participantContextId), any(), any())).thenReturn(ServiceResult.success(responseMessage));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));

        var response = controller().requestCredential(participantContextIdEncoded, createObjectBuilder().build(), "Bearer " + token);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString("Location")).contains("/v1alpha/participants/%s/requests/%s".formatted(participantContextIdEncoded, responseMessage.requestId()));

        verify(dcpIssuerTokenVerifier).verify(any(), argThat(tr -> tr.getToken().equals(token)));
        verify(dcpIssuerService).initiateCredentialsIssuance(participantContextId, requestMessage, ctx);
    }

    @Override
    protected CredentialRequestApiController controller() {
        return new CredentialRequestApiController(participantContextService, dcpIssuerService, dcpIssuerTokenVerifier, validatorRegistryMock, typeTransformerRegistry, namespace);
    }


    private CredentialRequestMessage createCredentialRequestMessage() {
        return createCredentialRequestMessageBuilder()
                .credential(new CredentialRequestSpecifier("test-credential1"))
                .build();
    }

    private CredentialRequestMessage.Builder createCredentialRequestMessageBuilder() {
        return CredentialRequestMessage.Builder.newInstance();
    }

    private ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(participantContextId)
                .did("did")
                .apiTokenAlias("apiTokenAlias")
                .build();
    }

    private Holder createHolder(String id, String did, String name) {
        return Holder.Builder.newInstance()
                .participantContextId(UUID.randomUUID().toString())
                .holderId(id)
                .did(did)
                .holderName(name)
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