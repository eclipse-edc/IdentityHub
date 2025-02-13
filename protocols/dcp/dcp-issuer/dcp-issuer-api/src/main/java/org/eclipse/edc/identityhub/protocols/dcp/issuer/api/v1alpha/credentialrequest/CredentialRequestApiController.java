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

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.net.URI;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/credentials")
public class CredentialRequestApiController implements CredentialRequestApi {

    private final DcpIssuerService dcpIssuerService;
    private final DcpHolderTokenVerifier tokenValidator;
    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry dcpTransformerRegistry;
    private final JsonLdNamespace namespace;

    public CredentialRequestApiController(DcpIssuerService dcpIssuerService,
                                          DcpHolderTokenVerifier tokenValidator,
                                          JsonObjectValidatorRegistry validatorRegistry,
                                          TypeTransformerRegistry dcpTransformerRegistry,
                                          JsonLdNamespace namespace) {
        this.dcpIssuerService = dcpIssuerService;
        this.tokenValidator = tokenValidator;
        this.validatorRegistry = validatorRegistry;
        this.dcpTransformerRegistry = dcpTransformerRegistry;
        this.namespace = namespace;
    }


    @POST
    @Path("/")
    @Override
    public Response requestCredential(JsonObject message, @HeaderParam(AUTHORIZATION) String token) {
        if (token == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        token = token.replace("Bearer", "").trim();

        validatorRegistry.validate(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_TERM), message).orElseThrow(ValidationFailureException::new);

        var credentialMessage = dcpTransformerRegistry.transform(message, CredentialRequestMessage.class).orElseThrow(InvalidRequestException::new);
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();


        var participant = tokenValidator.verify(tokenRepresentation)
                .orElseThrow((f) -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        return dcpIssuerService.initiateCredentialsIssuance(credentialMessage, participant)
                .map(response -> Response.created(URI.create("/v1alpha/requests/" + response.requestId())).build())
                .orElseThrow(exceptionMapper(CredentialRequestMessage.class, null));

    }
}
