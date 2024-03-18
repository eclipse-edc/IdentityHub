/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.v1;

import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Optional;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.ParticipantContextId.onEncoded;
import static org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantId}/presentations")
public class PresentationApiController implements PresentationApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final CredentialQueryResolver queryResolver;
    private final AccessTokenVerifier accessTokenVerifier;
    private final VerifiablePresentationService verifiablePresentationService;
    private final Monitor monitor;
    private final ParticipantContextService participantContextService;

    public PresentationApiController(JsonObjectValidatorRegistry validatorRegistry, TypeTransformerRegistry transformerRegistry, CredentialQueryResolver queryResolver,
                                     AccessTokenVerifier accessTokenVerifier, VerifiablePresentationService verifiablePresentationService, Monitor monitor, ParticipantContextService participantContextService) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.queryResolver = queryResolver;
        this.accessTokenVerifier = accessTokenVerifier;
        this.verifiablePresentationService = verifiablePresentationService;
        this.monitor = monitor;
        this.participantContextService = participantContextService;
    }


    @POST
    @Path("/query")
    @Override
    public Response queryPresentation(@PathParam("participantId") String participantContextId, JsonObject query, @HeaderParam(AUTHORIZATION) String token) {
        if (token == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }

        token = token.replace("Bearer", "").trim();

        validatorRegistry.validate(PRESENTATION_QUERY_MESSAGE_TYPE_PROPERTY, query).orElseThrow(ValidationFailureException::new);

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var presentationQuery = transformerRegistry.transform(query, PresentationQueryMessage.class).orElseThrow(InvalidRequestException::new);

        if (presentationQuery.getPresentationDefinition() != null) {
            monitor.warning("DIF Presentation Queries are not supported yet. This will get implemented in future iterations.");
            return notImplemented();
        }

        // verify that the participant actually exists
        participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));


        // verify and validate the requestor's SI token
        var issuerScopes = accessTokenVerifier.verify(token, participantContextId).orElseThrow(f -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        // query the database
        var credentials = queryResolver.query(participantContextId, presentationQuery, issuerScopes).orElseThrow(f -> new NotAuthorizedException(f.getFailureDetail()));

        // package the credentials in a VP and sign
        var audience = getAudience(token);
        var presentationResponse = verifiablePresentationService.createPresentation(participantContextId, credentials.toList(), presentationQuery.getPresentationDefinition(), audience)
                .compose(presentation -> transformerRegistry.transform(presentation, JsonObject.class))
                .orElseThrow(failure -> new EdcException("Error creating VerifiablePresentation: %s".formatted(failure.getFailureDetail())));
        return Response.ok()
                .entity(presentationResponse)
                .build();
    }

    private @Nullable String getAudience(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getJWTClaimsSet().getClaim(JwtRegisteredClaimNames.ISSUER))
                    .map(Object::toString)
                    .orElse(null);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Response notImplemented() {
        var error = ApiErrorDetail.Builder.newInstance()
                .message("Not implemented.")
                .type("Not implemented.")
                .build();
        return Response.status(503)
                .entity(error)
                .build();
    }

}
