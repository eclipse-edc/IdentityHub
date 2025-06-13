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

package org.eclipse.edc.identityhub.api.verifiablecredential;

import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_0_8;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/presentations")
public class PresentationApiController implements PresentationApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final CredentialQueryResolver queryResolver;
    private final SelfIssuedTokenVerifier selfIssuedTokenVerifier;
    private final VerifiablePresentationService verifiablePresentationService;
    private final Monitor monitor;
    private final ParticipantContextService participantContextService;
    private final JsonLd jsonLd;

    private final Map<JsonLdNamespace, String> protocols = Map.of(
            DSPACE_DCP_NAMESPACE_V_0_8, DCP_SCOPE_V_0_8,
            DSPACE_DCP_NAMESPACE_V_1_0, DCP_SCOPE_V_1_0
    );

    public PresentationApiController(JsonObjectValidatorRegistry validatorRegistry, TypeTransformerRegistry transformerRegistry, CredentialQueryResolver queryResolver,
                                     SelfIssuedTokenVerifier selfIssuedTokenVerifier, VerifiablePresentationService verifiablePresentationService, Monitor monitor, ParticipantContextService participantContextService, JsonLd jsonLd) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.queryResolver = queryResolver;
        this.selfIssuedTokenVerifier = selfIssuedTokenVerifier;
        this.verifiablePresentationService = verifiablePresentationService;
        this.monitor = monitor;
        this.participantContextService = participantContextService;
        this.jsonLd = jsonLd;
    }


    @POST
    @Path("/query")
    @Override
    public Response queryPresentation(@PathParam("participantContextId") String participantContextId, JsonObject query, @HeaderParam(AUTHORIZATION) String token) {
        if (token == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }

        if (!token.startsWith("Bearer")) {
            throw new AuthenticationFailedException("Authorization header must start with 'Bearer'");
        }

        token = token.replace("Bearer", "").trim();

        query = jsonLd.expand(query).orElseThrow(InvalidRequestException::new);

        var protocol = parseProtocol(query).orElseThrow(InvalidRequestException::new);

        validatorRegistry.validate(protocol.namespace().toIri(PRESENTATION_QUERY_MESSAGE_TERM), query).orElseThrow(ValidationFailureException::new);

        var protocolRegistry = transformerRegistry.forContext(protocol.scope());

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var presentationQuery = protocolRegistry.forContext(protocol.scope()).transform(query, PresentationQueryMessage.class).orElseThrow(InvalidRequestException::new);

        if (presentationQuery.getPresentationDefinition() != null) {
            monitor.warning("DIF Presentation Queries are not supported yet. This will get implemented in future iterations.");
            return notImplemented();
        }

        // verify that the participant actually exists
        participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));


        // verify and validate the requestor's SI token
        var issuerScopes = selfIssuedTokenVerifier.verify(token, participantContextId).orElseThrow(f -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        // query the database
        var credentials = queryResolver.query(participantContextId, presentationQuery, issuerScopes).orElseThrow(f -> new NotAuthorizedException(f.getFailureDetail()));

        // package the credentials in a VP and sign
        var audience = getAudience(token);
        var presentationResponse = verifiablePresentationService.createPresentation(participantContextId, credentials.toList(), presentationQuery.getPresentationDefinition(), audience)
                .compose(presentation -> protocolRegistry.transform(presentation, JsonObject.class))
                .compose(json -> jsonLd.compact(json, protocol.scope()))
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

    private Result<DcpProtocol> parseProtocol(JsonObject query) {
        var type = query.getJsonArray(JsonLdKeywords.TYPE);
        if (type == null) {
            return Result.failure("Missing type in query");
        }
        return protocols.entrySet().stream()
                .filter(p -> type.contains(Json.createValue(p.getKey().toIri(PRESENTATION_QUERY_MESSAGE_TERM))))
                .map(entry -> new DcpProtocol(entry.getKey(), entry.getValue()))
                .map(Result::success)
                .findAny()
                .orElseGet(() -> Result.failure("Unsupported protocol"));
    }

    private Response notImplemented() {
        var error = ApiErrorDetail.Builder.newInstance()
                .message("Not implemented.")
                .type("Not implemented.")
                .build();
        return Response.status(501)
                .entity(error)
                .build();
    }

    private record DcpProtocol(JsonLdNamespace namespace, String scope) {

    }

}
