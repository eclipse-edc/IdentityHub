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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DcpCredentialStorageClientTest {

    private static final String PARTICIPANT_CONTEXT_ID = "issuer-context-id";
    private static final String ISSUER_DID = "did:web:issuer";
    private static final String HOLDER_ID = "holder-id";
    private static final String HOLDER_DID = "did:web:holder";
    private static final String CREDENTIAL_SERVICE_URL = "https://holder.com/csvc";

    private final EdcHttpClient httpClient = mock();
    private final ParticipantContextStore participantContextStore = mock();
    private final HolderStore holderStore = mock();
    private final CredentialServiceUrlResolver credentialServiceUrlResolver = mock();
    private final ParticipantSecureTokenService secureTokenService = mock();
    private final TypeManager typeManager = mock();

    private final DcpCredentialStorageClient client = new DcpCredentialStorageClient(httpClient, participantContextStore,
            holderStore, credentialServiceUrlResolver, secureTokenService, mock(), typeManager, "test");

    @BeforeEach
    void setUp() throws IOException {
        when(typeManager.getMapper("test")).thenReturn(new ObjectMapper());
        when(participantContextStore.findById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success(participantContext()));
        when(holderStore.findById(HOLDER_ID)).thenReturn(StoreResult.success(holder()));
        when(credentialServiceUrlResolver.resolve(HOLDER_DID)).thenReturn(Result.success(CREDENTIAL_SERVICE_URL));
        when(secureTokenService.createToken(anyString(), anyMap(), isNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("si-token").build()));
        when(httpClient.execute(any(Request.class))).thenReturn(response(200));
    }

    // B5.5: successful delivery POSTs a CredentialMessage to <credentialService>/credentials with issuerPid = process id,
    // holderPid, status ISSUED, and an Authorization Bearer header carrying the SI token
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_success() throws IOException {
        var process = issuanceProcess();

        var result = client.deliverCredentials(process, List.of(credential()));

        assertThat(result).isSucceeded();

        var captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).execute(captor.capture());
        var request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.url().toString()).isEqualTo(CREDENTIAL_SERVICE_URL + "/credentials");
        assertThat(request.header("Authorization")).isEqualTo("Bearer si-token");
        // TODO: parse the request body and assert issuerPid == process.getId(), holderPid == process.getHolderPid(),
        //  status == "ISSUED", and one credential entry with the DCP profile string as 'format' and the raw VC as 'payload'
        // TODO: capture the claims passed to secureTokenService.createToken() and assert iss == sub == issuer DID,
        //  aud == holder DID, exp ~ +5min
    }

    // B5.5: participant context not found -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenParticipantContextNotFound_returnsFailure() {
        when(participantContextStore.findById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.notFound("not found"));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("Participant context not found");
    }

    // B5.5 / B9.2: holder not found (e.g. deleted between request acceptance and delivery) -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenHolderNotFound_returnsFailure() {
        when(holderStore.findById(HOLDER_ID)).thenReturn(StoreResult.notFound("not found"));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("Participant not found");
    }

    // B5.5: holder DID document has no CredentialService endpoint -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenNoCredentialServiceEndpoint_returnsFailure() {
        when(credentialServiceUrlResolver.resolve(HOLDER_DID)).thenReturn(Result.failure("no CredentialService entry"));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("Credential service URL not found");
    }

    // B5.5: STS token creation failure -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenStsTokenCreationFails_returnsFailure() {
        when(secureTokenService.createToken(anyString(), anyMap(), isNull())).thenReturn(Result.failure("sts failure"));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("Error creating self-issued token");
    }

    // B5.5: HTTP non-2xx response from the holder's Storage API -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenHttpNon2xx_returnsFailure() throws IOException {
        when(httpClient.execute(any(Request.class))).thenReturn(response(500));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("HTTP 500");
    }

    // B5.5: IOException while sending the CredentialMessage -> failure
    @Disabled("TODO: implement (catalog B5.5)")
    @Test
    void deliverCredentials_whenIoException_returnsFailure() throws IOException {
        when(httpClient.execute(any(Request.class))).thenThrow(new IOException("connection reset"));

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isFailed().detail().contains("connection reset");
    }

    // B5.4: when the holder's original request SI token contained a 'token' claim (access token), the delivery SI token
    // MUST carry that same access token in its own 'token' claim (DCP spec requirement)
    @Disabled("documents intended behavior, not yet implemented (catalog B5.4)")
    @Test
    void deliverCredentials_shouldEchoAccessTokenFromOriginalRequest() {
        // TODO: the access token from the holder's original CredentialRequestMessage SI token is currently not persisted
        //  on the IssuanceProcess, so it cannot be propagated here - production code must be extended first

        var result = client.deliverCredentials(issuanceProcess(), List.of(credential()));

        assertThat(result).isSucceeded();
        // TODO: capture the claims passed to secureTokenService.createToken() and assert they contain a 'token' claim
        //  equal to the access token from the holder's original request SI token
    }

    private ParticipantContext participantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .identity(ISSUER_DID)
                .state(ParticipantContextState.ACTIVATED)
                .build();
    }

    private Holder holder() {
        return Holder.Builder.newInstance()
                .holderId(HOLDER_ID)
                .did(HOLDER_DID)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .build();
    }

    private IssuanceProcess issuanceProcess() {
        return IssuanceProcess.Builder.newInstance()
                .id("issuance-process-id")
                .state(IssuanceProcessStates.APPROVED.code())
                .holderId(HOLDER_ID)
                .holderPid("holder-pid")
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .credentialFormats(Map.of("definition-id", VC1_0_JWT))
                .build();
    }

    private VerifiableCredentialContainer credential() {
        return new VerifiableCredentialContainer("rawVc", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .type("MembershipCredential")
                .issuer(new Issuer(ISSUER_DID))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(HOLDER_DID)
                        .claims(Map.of("member", "Alice"))
                        .build())
                .build());
    }

    private Response response(int code) {
        return new Response.Builder()
                .request(new Request.Builder().url(CREDENTIAL_SERVICE_URL + "/credentials").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .build();
    }
}
