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

import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialDescriptor;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
class IssuerCredentialOfferServiceImplTest {

    private static final String HOLDER_ID = "holder-id";
    private static final String PARTICIPANT_CONTEXT_ID = "issuer-context-id";
    private static final String HOLDER_CS_ENDPOINT = "https://holder.com/credentialservice";
    private final HolderStore holderStore = mock();
    private final CredentialServiceUrlResolver credentialServiceUrlResolver = mock();
    private final ParticipantSecureTokenService sts = mock();
    private final ParticipantContextService participantContextService = mock();
    private final EdcHttpClient httpClient = mock();

    private final IssuerCredentialOfferService credentialOfferService = new IssuerCredentialOfferServiceImpl(new NoopTransactionContext(),
            holderStore,
            credentialServiceUrlResolver,
            sts,
            participantContextService,
            mock(),
            httpClient);

    @BeforeEach
    void setUp() {
        when(httpClient.execute(any(), (Function<Response, Result<String>>) any())).thenReturn(success("{}"));
        when(holderStore.findById(HOLDER_ID)).thenReturn(StoreResult.success(holder()));
        when(sts.createToken(anyString(), anyMap(), isNull())).thenReturn(success(TokenRepresentation.Builder.newInstance().token("test-token").build()));
        when(credentialServiceUrlResolver.resolve(anyString())).thenReturn(success(HOLDER_CS_ENDPOINT));
        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID))).thenReturn(ServiceResult.success(issuerParticipant()));
    }

    @Test
    void sendCredentialOffer_success() {
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isSucceeded();
        verify(holderStore).findById(eq(HOLDER_ID));
        verify(participantContextService).getParticipantContext(eq(PARTICIPANT_CONTEXT_ID));
        verify(sts).createToken(anyString(), anyMap(), isNull());
        verify(credentialServiceUrlResolver).resolve(anyString());
        verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
        verifyNoMoreInteractions(holderStore, participantContextService, sts, httpClient);
    }

    @Test
    void sendCredentialOffer_holderNotExist() {
        when(holderStore.findById(HOLDER_ID)).thenReturn(StoreResult.notFound("foobar"));
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isFailed().detail().contains("foobar");
        verify(holderStore).findById(eq(HOLDER_ID));
        verifyNoMoreInteractions(participantContextService, credentialServiceUrlResolver, sts, httpClient);

    }

    @Test
    void sendCredentialOffer_offerRequestFailure() {
        when(httpClient.execute(any(), (Function<Response, Result<String>>) any())).thenReturn(Result.failure("not reachable"));
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isFailed().detail().contains("not reachable");
        verify(holderStore).findById(eq(HOLDER_ID));
        verify(participantContextService).getParticipantContext(eq(PARTICIPANT_CONTEXT_ID));
        verify(credentialServiceUrlResolver).resolve(anyString());
        verify(sts).createToken(anyString(), anyMap(), isNull());
        verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
        verifyNoMoreInteractions(holderStore, participantContextService, credentialServiceUrlResolver, sts, httpClient);
    }

    @Test
    void sendCredentialOffer_participantContextNotExist() {
        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID))).thenReturn(ServiceResult.notFound("not found"));
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isFailed().detail().contains("not found");
        verify(holderStore).findById(eq(HOLDER_ID));
        verify(participantContextService).getParticipantContext(eq(PARTICIPANT_CONTEXT_ID));
        verifyNoMoreInteractions(holderStore, participantContextService, credentialServiceUrlResolver, sts, httpClient);
    }

    @Test
    void sendCredentialOffer_holderDidNotResolvable() {
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.failure("not resolvable"));
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isFailed().detail().contains("not resolvable");
        verify(holderStore).findById(eq(HOLDER_ID));
        verify(participantContextService).getParticipantContext(eq(PARTICIPANT_CONTEXT_ID));
        verify(credentialServiceUrlResolver).resolve(anyString());
        verifyNoMoreInteractions(holderStore, participantContextService, credentialServiceUrlResolver, sts, httpClient);
    }

    @Test
    void sendCredentialOffer_stsFails() {
        when(sts.createToken(anyString(), anyMap(), isNull())).thenReturn(Result.failure("random STS failure"));
        var result = credentialOfferService.sendCredentialOffer(PARTICIPANT_CONTEXT_ID, HOLDER_ID, List.of(new CredentialDescriptor(CredentialFormat.VC1_0_JWT, "TestCredential")));

        assertThat(result).isFailed().detail().contains("random STS failure");
        verify(holderStore).findById(eq(HOLDER_ID));
        verify(participantContextService).getParticipantContext(eq(PARTICIPANT_CONTEXT_ID));
        verify(credentialServiceUrlResolver).resolve(anyString());
        verify(sts).createToken(anyString(), anyMap(), isNull());
        verifyNoMoreInteractions(holderStore, participantContextService, sts, httpClient);
    }

    @Test
    void sendCredentialOffer_webRequestFails() {
    }

    private ParticipantContext issuerParticipant() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .apiTokenAlias("test-token")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .build();
    }

    private Holder holder() {
        return Holder.Builder.newInstance()
                .did("did:web:" + HOLDER_ID)
                .holderId(HOLDER_ID)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .build();
    }
}
