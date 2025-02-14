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

package org.eclipse.edc.identityhub.api.storage;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/participants/{participantContextId}/credentials")
public class StorageApiController implements StorageApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final SelfIssuedTokenVerifier selfIssuedTokenVerifier;
    private final JsonLd jsonLd;

    public StorageApiController(JsonObjectValidatorRegistry validatorRegistry, TypeTransformerRegistry transformerRegistry,
                                SelfIssuedTokenVerifier selfIssuedTokenVerifier, JsonLd jsonLd) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.selfIssuedTokenVerifier = selfIssuedTokenVerifier;
        this.jsonLd = jsonLd;
    }


    @POST
    @Override
    public Response storeCredential(@PathParam("participantContextId") String participantContextId, JsonObject credentialMessageJson, @HeaderParam(AUTHORIZATION) String authHeader) {
        if (authHeader == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        var authtoken = authHeader.replace("Bearer", "").trim();
        credentialMessageJson = jsonLd.expand(credentialMessageJson).orElseThrow(InvalidRequestException::new);
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_MESSAGE_TERM), credentialMessageJson).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        var credentialMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(credentialMessageJson, CredentialMessage.class).orElseThrow(InvalidRequestException::new);

        var issuerScopes = selfIssuedTokenVerifier.verify(authtoken, participantContextId).orElseThrow(f -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        //todo: implement credential write

        return Response.ok().build();
    }

}
