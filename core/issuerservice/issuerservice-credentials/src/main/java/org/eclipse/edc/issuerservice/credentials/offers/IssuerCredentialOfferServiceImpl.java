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

package org.eclipse.edc.issuerservice.credentials.offers;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialDescriptor;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class IssuerCredentialOfferServiceImpl implements IssuerCredentialOfferService {

    private final TransactionContext transactionContext;
    private final HolderStore holderStore;
    private final CredentialServiceUrlResolver credentialServiceUrlResolver;
    private final ParticipantSecureTokenService secureTokenService;
    private final ParticipantContextService participantContextService;
    private final Monitor monitor;
    private final EdcHttpClient httpClient;

    public IssuerCredentialOfferServiceImpl(TransactionContext transactionContext,
                                            HolderStore holderStore,
                                            CredentialServiceUrlResolver credentialServiceUrlResolver,
                                            ParticipantSecureTokenService secureTokenService,
                                            ParticipantContextService participantContextService,
                                            EdcHttpClient httpClient, Monitor monitor) {
        this.transactionContext = transactionContext;
        this.holderStore = holderStore;
        this.credentialServiceUrlResolver = credentialServiceUrlResolver;
        this.secureTokenService = secureTokenService;
        this.participantContextService = participantContextService;
        this.monitor = monitor;
        this.httpClient = httpClient;
    }

    @Override
    public ServiceResult<Void> sendCredentialOffer(String participantContextId, String holderId, Collection<CredentialDescriptor> credentialDescriptor) {
        return transactionContext.execute(() -> {
            var holder = holderStore.findById(holderId);
            if (holder.failed()) {
                return ServiceResult.from(holder.mapFailure());
            }
            var holderDid = holder.getContent().getDid();
            return participantContextService.getParticipantContext(participantContextId)
                    .map(ParticipantContext::getDid)
                    .compose(issuerDid -> {
                        var requestResult = credentialServiceUrlResolver.resolve(holderDid)
                                .compose(url -> createOfferMessageRequest(url, participantContextId, issuerDid, holderDid, credentialDescriptor));
                        return ServiceResult.from(requestResult);
                    })
                    .compose(this::sendRequest)
                    .mapEmpty();
        });
    }


    private ServiceResult<String> sendRequest(Request request) {
        monitor.warning("Sending CredentialOffers is currently not implemented. This method will return without any network interaction.");
        return ServiceResult.from(httpClient.execute(request, this::mapResponse));
    }

    private Result<String> mapResponse(Response response) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body().string());
            }
            return Result.failure("Response not successful or body is empty");
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    private Result<Request> createOfferMessageRequest(String credentialServiceUrl, String participantContextId, String issuerDid, String holderDid, Collection<CredentialDescriptor> credentialDescriptor) {
        var url = credentialServiceUrl + OFFER_ENDPOINT;
        return getAuthToken(participantContextId, holderDid, issuerDid)
                .map(token -> new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token.getToken())
                        .post(RequestBody.create(createOfferMessage(issuerDid, credentialDescriptor).toString(), MediaType.parse("application/json")))
                        .build());
    }

    private JsonObject createOfferMessage(String issuerDid, Collection<CredentialDescriptor> credentialDescriptor) {
        return Json.createObjectBuilder().build();
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
