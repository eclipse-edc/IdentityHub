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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequest;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class CredentialRequestServiceImpl implements CredentialRequestService {
    private final HolderCredentialRequestStore holderCredentialRequestStore;
    private final DidResolverRegistry didResolverRegistry;
    private final TypeTransformerRegistry dcpTypeTransformerRegistry;
    private final EdcHttpClient httpClient;
    private final SecureTokenService secureTokenService;
    private final String ownDid;

    public CredentialRequestServiceImpl(HolderCredentialRequestStore holderCredentialRequestStore,
                                        DidResolverRegistry didResolverRegistry,
                                        TypeTransformerRegistry dcpTypeTransformerRegistry,
                                        EdcHttpClient httpClient,
                                        SecureTokenService secureTokenService,
                                        String ownDid) {
        this.holderCredentialRequestStore = holderCredentialRequestStore;
        this.didResolverRegistry = didResolverRegistry;
        this.dcpTypeTransformerRegistry = dcpTypeTransformerRegistry;
        this.httpClient = httpClient;
        this.secureTokenService = secureTokenService;
        this.ownDid = ownDid;
    }

    @Override
    public ServiceResult<String> initiateRequest(String participantContext, String issuerDid, String requestId, Map<String, String> typesAndFormats) {

        var newRequest = HolderCredentialRequest.Builder.newInstance()
                .id(requestId)
                .issuerDid(issuerDid)
                .credentialTypes(typesAndFormats.keySet().stream().toList())
                .participantContext(participantContext)
                .state(HolderRequestState.CREATED.code())
                .build();

        var result = didResolverRegistry.resolve(issuerDid)
                .compose(this::getCredentialRequestEndpoint)
                .map(credentialRequestEndpoint -> {
                    holderCredentialRequestStore.save(newRequest);
                    return credentialRequestEndpoint;
                })
                .compose(endpoint -> {
                    var rq = newRequest.copy().toBuilder()
                            .state(HolderRequestState.REQUESTING.code())
                            .build();
                    holderCredentialRequestStore.save(rq); // set state
                    return getAuthToken(issuerDid, ownDid).compose(token -> sendCredentialsRequest(token, endpoint, requestId, typesAndFormats));
                })
                .onFailure(failure -> {
                    var rq = newRequest.copy().toBuilder()
                            .state(HolderRequestState.ERROR.code())
                            .errorDetail(failure.getFailureDetail())
                            .build();
                    holderCredentialRequestStore.save(rq);
                })
                .map(issuanceProcessId -> newRequest.copy().toBuilder()
                        .issuanceProcessId(issuanceProcessId)
                        .state(REQUESTED.code())
                        .build())
                .compose(rq -> {
                    holderCredentialRequestStore.save(rq);
                    return success(rq.getIssuanceProcessId());
                });

        return ServiceResult.from(result);
    }

    /**
     * send credential request message over DCP to the issuer endpoint
     *
     * @param token                    the token that should be used in the Authorization header of the DCP request
     * @param issuerRequestEndpointUrl the URL of the Issuer's Credential Request API endpoint
     * @param requestId                the request ID property that will be attached to the request
     * @param typesAndFormats          a map of credential-type-to-format entries. The credential-type is the entry's key, the format is the entry's value
     * @return a Result containing the Issuer-assigned issuance process ID
     */
    private Result<String> sendCredentialsRequest(TokenRepresentation token, String issuerRequestEndpointUrl, String requestId, Map<String, String> typesAndFormats) {
        var rqMessage = CredentialRequestMessage.Builder.newInstance();
        rqMessage.requestId(requestId);

        typesAndFormats.forEach((type, format) -> rqMessage.credential(new CredentialRequest(type, format, null)));


        var jsonObj = dcpTypeTransformerRegistry.transform(rqMessage.build(), JsonObject.class);

        return jsonObj.map(JsonObject::toString)
                .map(json -> new Request.Builder()
                        .url(issuerRequestEndpointUrl)
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .header("Authorization", "Bearer " + token.getToken())
                        .build())
                .compose(request -> httpClient.execute(request, this::mapResponse));

    }

    /**
     * maps a {@link Response} to a result containing the response body
     */
    private Result<String> mapResponse(Response response) {
        if (response.isSuccessful()) {
            if (response.body() != null) {
                try {
                    return success(response.body().string());
                } catch (IOException e) {
                    return failure(e.getMessage());
                }
            }
        }
        return failure("Error sending DCP Credential Request: code: '%s', message: '%s'".formatted(response.code(), response.message()));
    }

    /**
     * Fetches the authentication token from the SecureTokenService.
     *
     * @param audience the String used as {@code aud} claim
     * @param myOwnDid the String used as {@code iss} and {@code sub} claims
     * @return a JWT token that can be used to send DCP messages to the issuer
     */
    private Result<TokenRepresentation> getAuthToken(String audience, String myOwnDid) {
        var siTokenClaims = Map.of(
                ISSUED_AT, Instant.now().toString(),
                AUDIENCE, audience,
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        return secureTokenService.createToken(siTokenClaims, null);
    }

    /**
     * Extracts the {@code CredentialRequest} service endpoint from the DID document
     *
     * @param issuerDidDocument The Issuer's DID document
     * @return A result containing the service entry
     */
    private Result<String> getCredentialRequestEndpoint(DidDocument issuerDidDocument) {
        var endpoint = issuerDidDocument.getService().stream().filter(s -> s.getType().equalsIgnoreCase(ISSUER_SERVICE_ENDPOINT_TYPE)).findAny();

        return endpoint.map(s -> success((s.getServiceEndpoint() + "/credentials").replace("//", "/")))
                .orElseGet(() -> failure("The Issuer's DID Document does not contain any '%s' endpoint".formatted(ISSUER_SERVICE_ENDPOINT_TYPE)));
    }
}
