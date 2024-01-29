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

package org.eclipse.edc.identityservice.api.v1;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.api.v1.PresentationApiController;
import org.eclipse.edc.identityhub.spi.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identitytrust.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationResponseMessage;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.identitytrust.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.identityhub.spi.resolution.QueryResult.success;
import static org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
@SuppressWarnings("resource")
class PresentationApiControllerTest extends RestControllerTestBase {

    private final JsonObjectValidatorRegistry validatorRegistryMock = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final CredentialQueryResolver queryResolver = mock();
    private final AccessTokenVerifier accessTokenVerifier = mock();
    private final VerifiablePresentationService generator = mock();


    @Test
    void query_tokenNotPresent_shouldReturn401() {
        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authorization header missing");
    }

    @Test
    void query_validationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(failure(violation("foo", "bar")));

        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), generateJwt()))
                .isInstanceOf(ValidationFailureException.class)
                .hasMessage("foo");
    }

    @Test
    void query_transformationError_shouldReturn400() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.failure("cannot transform"));

        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), generateJwt()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("cannot transform");
        verifyNoInteractions(accessTokenVerifier, queryResolver, generator);
    }

    @Test
    void query_withPresentationDefinition_shouldReturn501() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder()
                .presentationDefinition(PresentationDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build());
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder.build()));

        var response = controller().queryPresentation(createObjectBuilder().build(), generateJwt());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getEntity()).extracting(o -> (ApiErrorDetail) o).satisfies(ed -> {
            assertThat(ed.getMessage()).isEqualTo("Not implemented.");
            assertThat(ed.getType()).isEqualTo("Not implemented.");
        });
        verifyNoInteractions(accessTokenVerifier, queryResolver, generator);
    }


    @Test
    void query_tokenVerificationFails_shouldReturn401() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(accessTokenVerifier.verify(anyString())).thenReturn(Result.failure("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessage("ID token verification failed: test-failure");
        verifyNoInteractions(queryResolver, generator);
    }

    @Test
    void query_queryResolutionFails_shouldReturn403() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(accessTokenVerifier.verify(anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(any(), eq(List.of("test-scope1")))).thenReturn(QueryResult.unauthorized("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), generateJwt()))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("test-failure");
        verifyNoInteractions(generator);
    }

    @Test
    void query_presentationGenerationFails_shouldReturn500() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(accessTokenVerifier.verify(anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(any(), eq(List.of("test-scope1")))).thenReturn(success(Stream.empty()));

        when(generator.createPresentation(anyList(), any(), any())).thenReturn(Result.failure("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(createObjectBuilder().build(), generateJwt()))
                .isExactlyInstanceOf(EdcException.class)
                .hasMessage("Error creating VerifiablePresentation: test-failure");
    }

    @Test
    void query_success() {
        when(validatorRegistryMock.validate(eq(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(accessTokenVerifier.verify(anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(any(), eq(List.of("test-scope1")))).thenReturn(success(Stream.empty()));

        var pres = PresentationResponseMessage.Builder.newinstance().presentation(List.of(generateJwt()))
                .presentationSubmission(new PresentationSubmission("id", "def-id", List.of(new InputDescriptorMapping("id", "ldp_vp", "$.verifiableCredentials[0]"))))
                .build();
        when(generator.createPresentation(anyList(), any(), any())).thenReturn(Result.success(pres));

        var response = controller().queryPresentation(createObjectBuilder().build(), generateJwt());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(pres);

    }

    @Override
    protected PresentationApiController controller() {
        return new PresentationApiController(validatorRegistryMock, typeTransformerRegistry, queryResolver, accessTokenVerifier, generator, mock());
    }

    private String generateJwt() {
        var ecKey = generateEcKey();
        var jwt = buildSignedJwt(new JWTClaimsSet.Builder().audience("test-audience")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issuer("test-issuer")
                .subject("test-subject")
                .jwtID(UUID.randomUUID().toString()).build(), ecKey);

        return jwt.serialize();
    }

    private PresentationQueryMessage.Builder createPresentationQueryBuilder() {
        return PresentationQueryMessage.Builder.newinstance()
                .scopes(List.of("test-scope1", "test-scope2"));
    }
}