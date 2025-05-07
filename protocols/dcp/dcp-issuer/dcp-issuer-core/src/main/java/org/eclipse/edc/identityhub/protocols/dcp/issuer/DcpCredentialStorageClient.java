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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.TYPE_TERM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;

public class DcpCredentialStorageClient implements CredentialStorageClient {
    public static final String STORAGE_ENDPOINT = "/credentials";
    private final EdcHttpClient httpClient;
    private final ParticipantContextStore participantContextStore;
    private final HolderStore holderStore;
    private final CredentialServiceUrlResolver credentialServiceUrlResolver;
    private final ParticipantSecureTokenService secureTokenService;
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final String typeContext;

    public DcpCredentialStorageClient(EdcHttpClient httpClient, ParticipantContextStore participantContextStore,
                                      HolderStore holderStore, CredentialServiceUrlResolver credentialServiceUrlResolver,
                                      ParticipantSecureTokenService secureTokenService, Monitor monitor, TypeManager typeManager, String typeContext) {
        this.httpClient = httpClient;
        this.participantContextStore = participantContextStore;
        this.holderStore = holderStore;
        this.credentialServiceUrlResolver = credentialServiceUrlResolver;
        this.secureTokenService = secureTokenService;
        this.monitor = monitor;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public Result<Void> deliverCredentials(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> credentials) {

        try {
            var issuerDid = participantContextStore.findById(issuanceProcess.getParticipantContextId()).map(ParticipantContext::getDid)
                    .orElseThrow(failure -> new EdcException("Participant context not found"));
            var participantDid = holderStore.findById(issuanceProcess.getHolderId()).map(Holder::getDid)
                    .orElseThrow(failure -> new EdcException("Participant not found"));

            var credentialServiceBaseUrl = credentialServiceUrlResolver.resolve(participantDid)
                    .orElseThrow(failure -> new EdcException("Credential service URL not found"));
            var url = credentialServiceBaseUrl + STORAGE_ENDPOINT;

            var selfIssuedTokenJwt = getAuthToken(issuanceProcess.getParticipantContextId(), participantDid, issuerDid)
                    .orElseThrow(failure -> new EdcException("Error creating self-issued token"));

            var credentialMessage = createCredentialMessage(issuanceProcess, credentials);

            return sendRequest(credentialMessage, url, selfIssuedTokenJwt);
        } catch (EdcException e) {
            monitor.warning("Error delivering credentials", e);
            return failure("Error delivering credentials: %s".formatted(e.getMessage()));
        }

    }

    private @NotNull Result<Void> sendRequest(JsonObject credentialMessage, String url, TokenRepresentation selfIssuedTokenJwt) {
        try {
            var requestJson = typeManager.getMapper(typeContext).writeValueAsString(credentialMessage);
            var request = new Request.Builder()
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .url(url)
                    .addHeader("Authorization", "Bearer %s".formatted(selfIssuedTokenJwt.getToken()))
                    .build();

            try (var response = httpClient.execute(request)) {

                if (response.isSuccessful()) {
                    return Result.success();
                }
                return failure("Credential Message failed: HTTP %s".formatted(response.code()));
            }


        } catch (IOException e) {
            monitor.warning("Error writing credentials", e);
            return failure("Error writing credentials: %s".formatted(e.getMessage()));
        }
    }

    private JsonObject createCredentialMessage(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> credentials) {
        return Json.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, Json.createArrayBuilder()
                        .add(DSPACE_DCP_V_1_0_CONTEXT))
                .add(TYPE_TERM, CREDENTIAL_MESSAGE_TERM)
                .add(ISSUER_PID_TERM, issuanceProcess.getId())
                .add(HOLDER_PID_TERM, issuanceProcess.getHolderPid())
                .add(CREDENTIALS_TERM, credentials.stream().map(this::toJson).collect(toJsonArray()))
                .add(STATUS_TERM, "ISSUED")
                .build();
    }

    private JsonObject toJson(VerifiableCredentialContainer credential) {
        return Json.createObjectBuilder()
                .add("credentialType", credential.credential().getType().stream().filter("VerifiableCredential"::equals).findFirst().orElseThrow())
                .add("format", credential.format().name())
                .add("payload", credential.rawVc())
                .build();
    }

    private Result<TokenRepresentation> getAuthToken(String participantContextId, String audience, String myOwnDid) {
        var siTokenClaims = Map.of(
                ISSUED_AT, Instant.now().toString(),
                AUDIENCE, audience,
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        return secureTokenService.createToken(participantContextId, siTokenClaims, null);
    }
}
