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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.api.verifiablecredential.PresentationApiController;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_0_8;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
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

    private static final String PARTICIPANT_ID = "participant-id";
    private final JsonObjectValidatorRegistry validatorRegistryMock = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final CredentialQueryResolver queryResolver = mock();
    private final SelfIssuedTokenVerifier selfIssuedTokenVerifier = mock();
    private final VerifiablePresentationService generator = mock();
    private final JsonLd jsonLd = mock();
    private final ParticipantContextService participantContextService = mock(a -> ServiceResult.success(ParticipantContext.Builder.newInstance()
            .participantContextId(a.getArgument(0).toString())
            .apiTokenAlias("test-alias")
            .build()));

    @Test
    void query_tokenNotPresent_shouldReturn401() {
        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authorization header missing");
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_validationError_shouldReturn400(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(failure(violation("foo", "bar")));

        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken()))
                .isInstanceOf(ValidationFailureException.class)
                .hasMessage("foo");
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_transformationError_shouldReturn400(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.failure("cannot transform"));

        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("cannot transform");
        verifyNoInteractions(selfIssuedTokenVerifier, queryResolver, generator);
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_withPresentationDefinition_shouldReturn501(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder()
                .presentationDefinition(PresentationDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build());
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder.build()));

        var response = controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken());
        assertThat(response.getStatus()).isEqualTo(501);
        assertThat(response.getEntity()).extracting(o -> (ApiErrorDetail) o).satisfies(ed -> {
            assertThat(ed.getMessage()).isEqualTo("Not implemented.");
            assertThat(ed.getType()).isEqualTo("Not implemented.");
        });
        verifyNoInteractions(selfIssuedTokenVerifier, queryResolver, generator);
    }


    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_tokenVerificationFails_shouldReturn401(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(selfIssuedTokenVerifier.verify(anyString(), anyString())).thenReturn(Result.failure("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessage("ID token verification failed: test-failure");
        verifyNoInteractions(queryResolver, generator);
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_queryResolutionFails_shouldReturn403(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(selfIssuedTokenVerifier.verify(anyString(), anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(anyString(), any(), eq(List.of("test-scope1")))).thenReturn(QueryResult.unauthorized("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken()))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("test-failure");
        verifyNoInteractions(generator);
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_presentationGenerationFails_shouldReturn500(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(selfIssuedTokenVerifier.verify(anyString(), anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(anyString(), any(), eq(List.of("test-scope1")))).thenReturn(QueryResult.success(Stream.empty()));

        when(generator.createPresentation(anyString(), anyList(), any(), any())).thenReturn(Result.failure("test-failure"));

        assertThatThrownBy(() -> controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken()))
                .isExactlyInstanceOf(EdcException.class)
                .hasMessage("Error creating VerifiablePresentation: test-failure");
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolProvider.class)
    void query_success(JsonLdNamespace namespace, String scope) {
        when(jsonLd.expand(isA(JsonObject.class))).thenReturn(Result.success(createExpandedPresentationQueryMessage(namespace)));
        when(validatorRegistryMock.validate(eq(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM)), any())).thenReturn(success());
        var presentationQueryBuilder = createPresentationQueryBuilder().build();
        when(typeTransformerRegistry.forContext(scope)).thenReturn(typeTransformerRegistry);
        when(typeTransformerRegistry.transform(isA(JsonObject.class), eq(PresentationQueryMessage.class))).thenReturn(Result.success(presentationQueryBuilder));
        when(selfIssuedTokenVerifier.verify(anyString(), anyString())).thenReturn(Result.success(List.of("test-scope1")));
        when(queryResolver.query(anyString(), any(), eq(List.of("test-scope1")))).thenReturn(QueryResult.success(Stream.empty()));
        when(jsonLd.compact(isA(JsonObject.class), eq(scope))).thenReturn(Result.success(Json.createObjectBuilder().build()));
        var pres = PresentationResponseMessage.Builder.newinstance().presentation(List.of(generateAuthToken()))

                .presentationSubmission(new PresentationSubmission("id", "def-id", List.of(new InputDescriptorMapping("id", "ldp_vp", "$.verifiableCredentials[0]"))))
                .build();

        var jsonResponse = Json.createObjectBuilder().build();
        when(typeTransformerRegistry.transform(eq(pres), eq(JsonObject.class))).thenReturn(Result.success(jsonResponse));
        when(generator.createPresentation(anyString(), anyList(), any(), any())).thenReturn(Result.success(pres));

        var response = controller().queryPresentation(PARTICIPANT_ID, createObjectBuilder().build(), generateAuthToken());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(jsonResponse);

    }

    @Override
    protected PresentationApiController controller() {
        return new PresentationApiController(validatorRegistryMock, typeTransformerRegistry, queryResolver, selfIssuedTokenVerifier, generator, mock(), participantContextService, jsonLd);
    }

    private String generateAuthToken() {
        var ecKey = generateEcKey(null);
        var jwt = buildSignedJwt(new JWTClaimsSet.Builder().audience("test-audience")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issuer("test-issuer")
                .subject("test-subject")
                .jwtID(UUID.randomUUID().toString()).build(), ecKey);

        return "Bearer " + jwt.serialize();
    }

    private JsonObject createExpandedPresentationQueryMessage(JsonLdNamespace namespace) {
        var type = Json.createArrayBuilder().add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_TERM));
        return Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, type).build();
    }

    private PresentationQueryMessage.Builder createPresentationQueryBuilder() {
        return PresentationQueryMessage.Builder.newinstance()
                .scopes(List.of("test-scope1", "test-scope2"));
    }

    public static class ProtocolProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(DSPACE_DCP_NAMESPACE_V_0_8, DCP_SCOPE_V_0_8),
                    Arguments.of(DSPACE_DCP_NAMESPACE_V_1_0, DCP_SCOPE_V_1_0));
        }
    }
}