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

import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toSet;
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
    private final TypeTransformerRegistry dcpTransformerRegistry;
    private final DcpIssuerMetadataService dcpIssuerMetadataService;

    public IssuerCredentialOfferServiceImpl(TransactionContext transactionContext,
                                            HolderStore holderStore,
                                            CredentialServiceUrlResolver credentialServiceUrlResolver,
                                            ParticipantSecureTokenService secureTokenService,
                                            ParticipantContextService participantContextService,
                                            EdcHttpClient httpClient, Monitor monitor,
                                            TypeTransformerRegistry dcpTransformerRegistry, DcpIssuerMetadataService dcpIssuerMetadataService) {
        this.transactionContext = transactionContext;
        this.holderStore = holderStore;
        this.credentialServiceUrlResolver = credentialServiceUrlResolver;
        this.secureTokenService = secureTokenService;
        this.participantContextService = participantContextService;
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.dcpTransformerRegistry = dcpTransformerRegistry;
        this.dcpIssuerMetadataService = dcpIssuerMetadataService;
    }

    @Override
    public ServiceResult<Void> sendCredentialOffer(String participantContextId, String holderId, Collection<String> credentialObjectIds) {
        return transactionContext.execute(() -> {
            var holder = holderStore.findById(holderId);
            if (holder.failed()) {
                return ServiceResult.from(holder.mapFailure());
            }
            var holderDid = holder.getContent().getDid();
            return participantContextService.getParticipantContext(participantContextId)
                    .compose(participantContext -> {

                        var requestResult =
                                // get credential objects based on IDs
                                getCredentialObjects(participantContext, credentialObjectIds)
                                        .compose(offeredCredentials -> credentialServiceUrlResolver.resolve(holderDid)
                                                .compose(url -> getAuthToken(participantContextId, holderDid, participantContext.getDid())
                                                        //compose CredentialOfferMessage
                                                        .compose(tokenRepresentation -> createOfferMessageRequest(url, participantContext.getDid(), offeredCredentials, tokenRepresentation.getToken()))));

                        return ServiceResult.from(requestResult);
                    })
                    .compose(this::sendRequest)
                    .mapEmpty();
        });
    }

    /**
     * Retrieves a list of {@link CredentialObject}s based on the provided IDs from the issuer's metadata. These
     * credential objects are the same ones that would be published in the IssuerMetadata API.
     * <p>
     * If no credential objects are found with the provided IDs, an error is returned.
     */
    private Result<Collection<CredentialObject>> getCredentialObjects(ParticipantContext participantContext, Collection<String> credentialObjectIds) {
        var metadata = dcpIssuerMetadataService.getIssuerMetadata(participantContext);
        if (metadata.failed()) {
            return Result.failure(metadata.getFailureDetail());
        }
        var offeredCredentialObjects = metadata.getContent().getCredentialsSupported().stream()
                .filter(co -> credentialObjectIds.contains(co.getId()))
                .collect(toSet());

        if (offeredCredentialObjects.isEmpty()) {
            return Result.failure("No credential objects found with any of the provided IDs: " + credentialObjectIds);
        }
        return Result.success(offeredCredentialObjects);
    }


    /**
     * Sends a request to the credential service URL with the provided request.
     * <p>
     * Note: This method currently does not perform any actual network interaction and will only log a warning.
     *
     * @param request the request to send
     * @return a ServiceResult containing the response body as a string
     */
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

    /**
     * Creates an HTTP request to send a CredentialOfferMessage to the holder.
     *
     * @param credentialServiceUrl the base URL of the credential service to which the offer message will be sent
     * @param issuerDid            the DID of the issuing entity creating the offer message
     * @param credentialObjects    a collection of {@link CredentialObject} instances to be included in the metadata structure
     * @param token                the self-issued ID token to authorize the request without "Bearer " prefix.
     * @return a {@link Result} containing the constructed {@link Request}, or a failure result if the offer message creation fails
     */
    private Result<Request> createOfferMessageRequest(String credentialServiceUrl, String issuerDid, Collection<CredentialObject> credentialObjects, String token) {
        var url = credentialServiceUrl + OFFER_ENDPOINT;
        var offerMessage = createOfferMessage(issuerDid, credentialObjects);
        if (offerMessage.failed()) {
            return offerMessage.mapFailure();
        }

        return offerMessage.map(JsonObject::toString)
                .map(jsonBody -> new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .build());
    }

    /**
     * Creates a CredentialOfferMessage using the provided issuer DID and credential objects
     * and transforms it into a JSON representation.
     *
     * @param issuerDid         the DID of the issuing entity creating the offer message
     * @param credentialObjects a collection of {@link CredentialObject} instances to include in the offer
     * @return a {@link Result} containing the transformed {@link JsonObject} or a failure.
     */
    private Result<JsonObject> createOfferMessage(String issuerDid, Collection<CredentialObject> credentialObjects) {
        var credentialOfferMessage = CredentialOfferMessage.Builder.newInstance()
                .issuer(issuerDid)
                .credentials(new ArrayList<>(credentialObjects))
                .build();

        return dcpTransformerRegistry.transform(credentialOfferMessage, JsonObject.class);

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
