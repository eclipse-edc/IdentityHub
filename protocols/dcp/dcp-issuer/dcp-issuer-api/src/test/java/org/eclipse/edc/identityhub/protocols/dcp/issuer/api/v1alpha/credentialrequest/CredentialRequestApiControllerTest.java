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
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerSelfIssuedTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequest;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
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
    private final DcpIssuerSelfIssuedTokenVerifier dcpIssuerTokenVerifier = mock();
    private final JsonLdNamespace namespace = DSPACE_DCP_NAMESPACE_V_1_0;

    @Test
    void requestCredential_tokenNotPresent_shouldReturn401() {
        assertThatThrownBy(() -> controller().requestCredential(createObjectBuilder().build(), null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authorization header missing");

        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier, typeTransformerRegistry);

    }

    @Test
    void requestCredential_validationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(failure(violation("foo", "bar")));

        assertThatThrownBy(() -> controller().requestCredential(createObjectBuilder().build(), generateJwt()))
                .isInstanceOf(ValidationFailureException.class)
                .hasMessage("foo");
        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier, typeTransformerRegistry);

    }

    @Test
    void requestCredential_transformationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.failure("cannot transform"));

        assertThatThrownBy(() -> controller().requestCredential(createObjectBuilder().build(), generateJwt()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("cannot transform");

        verifyNoInteractions(dcpIssuerService, dcpIssuerTokenVerifier);
    }

    @Test
    void requestCredential_tokenVerificationFails_shouldReturn401() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        when(dcpIssuerTokenVerifier.verify(any())).thenReturn(ServiceResult.unauthorized("unauthorized"));

        assertThatThrownBy(() -> controller().requestCredential(createObjectBuilder().build(), generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("unauthorized");

        verifyNoInteractions(dcpIssuerService);
    }

    @Test
    void requestCredential_initiateCredentialIssuanceFails_shouldReturn_401() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        var participant = new Participant("id", "did", "name");

        var ctx = new DcpRequestContext(participant, Map.of());
        var token = generateJwt();
        when(dcpIssuerTokenVerifier.verify(any())).thenReturn(ServiceResult.success(ctx));
        when(dcpIssuerService.initiateCredentialsIssuance(any(), any())).thenReturn(ServiceResult.unauthorized("cannot initiate unauthorized"));

        assertThatThrownBy(() -> controller().requestCredential(createObjectBuilder().build(), token))
                .isExactlyInstanceOf(NotAuthorizedException.class)
                .hasMessage("cannot initiate unauthorized");

        verify(dcpIssuerTokenVerifier).verify(argThat(tr -> tr.getToken().equals(token)));
        verify(dcpIssuerService).initiateCredentialsIssuance(requestMessage, ctx);
    }

    @Test
    void requestCredential() {
        when(validatorRegistryMock.validate(eq(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM)), any())).thenReturn(success());
        var requestMessage = createCredentialRequestMessage();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(CredentialRequestMessage.class))).thenReturn(Result.success(requestMessage));
        var participant = new Participant("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());

        var token = generateJwt();
        var responseMessage = new CredentialRequestMessage.Response(UUID.randomUUID().toString());
        when(dcpIssuerTokenVerifier.verify(any())).thenReturn(ServiceResult.success(ctx));
        when(dcpIssuerService.initiateCredentialsIssuance(any(), any())).thenReturn(ServiceResult.success(responseMessage));

        var response = controller().requestCredential(createObjectBuilder().build(), token);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString("Location")).contains("/v1alpha/requests/%s".formatted(responseMessage.requestId()));

        verify(dcpIssuerTokenVerifier).verify(argThat(tr -> tr.getToken().equals(token)));
        verify(dcpIssuerService).initiateCredentialsIssuance(requestMessage, ctx);
    }

    @Override
    protected CredentialRequestApiController controller() {
        return new CredentialRequestApiController(dcpIssuerService, dcpIssuerTokenVerifier, validatorRegistryMock, typeTransformerRegistry, namespace);
    }


    private CredentialRequestMessage createCredentialRequestMessage() {
        return createCredentialRequestMessageBuilder()
                .credential(new CredentialRequest("test-credential1", "test-issuer1", null))
                .build();
    }

    private CredentialRequestMessage.Builder createCredentialRequestMessageBuilder() {
        return CredentialRequestMessage.Builder.newInstance();
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