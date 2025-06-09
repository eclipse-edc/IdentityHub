/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.issuermetadata;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import java.sql.Date;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public class IssuerMetadataApiControllerTest extends RestControllerTestBase {

    private final ParticipantContextService participantContextService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DcpHolderTokenVerifier dcpIssuerTokenVerifier = mock();
    private final DcpIssuerMetadataService issuerMetadataService = mock();
    private final String participantContextId = "participantContextId";
    private final String participantContextIdEncoded = Base64.getEncoder().encodeToString(participantContextId.getBytes());

    @ParameterizedTest
    @NullSource
    @EmptySource
    void issuerMetadata_noAuthToken_success(String emptyAuthHeader) {
        var participant = createHolder("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());
        var object = Json.createObjectBuilder().build();

        var metadata = IssuerMetadata.Builder.newInstance().build();
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));
        when(issuerMetadataService.getIssuerMetadata(argThat(p -> p.getParticipantContextId().equals(participantContextId)))).thenReturn(ServiceResult.success(metadata));
        when(typeTransformerRegistry.transform(eq(metadata), eq(JsonObject.class))).thenReturn(Result.success(object));
        var response = controller().getIssuerMetadata(participantContextIdEncoded, emptyAuthHeader);

        assertThat(response).isEqualTo(object);
    }

    @Test
    void issuerMetadata_participantNotFound_shouldReturn401() {
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller().getIssuerMetadata(participantContextIdEncoded, generateJwt()))
                .isExactlyInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid issuer");

        verifyNoInteractions(issuerMetadataService, dcpIssuerTokenVerifier);
    }

    @Test
    void issuerMetadata() {

        var participant = createHolder("id", "did", "name");
        var ctx = new DcpRequestContext(participant, Map.of());
        var object = Json.createObjectBuilder().build();

        var metadata = IssuerMetadata.Builder.newInstance().build();
        when(dcpIssuerTokenVerifier.verify(any(), any())).thenReturn(ServiceResult.success(ctx));
        when(participantContextService.getParticipantContext(eq(participantContextId))).thenReturn(ServiceResult.success(createParticipantContext()));
        when(issuerMetadataService.getIssuerMetadata(argThat(p -> p.getParticipantContextId().equals(participantContextId)))).thenReturn(ServiceResult.success(metadata));
        when(typeTransformerRegistry.transform(eq(metadata), eq(JsonObject.class))).thenReturn(Result.success(object));
        var response = controller().getIssuerMetadata(participantContextIdEncoded, generateJwt());

        assertThat(response).isEqualTo(object);

    }

    @Override
    protected IssuerMetadataApiController controller() {
        return new IssuerMetadataApiController(participantContextService, issuerMetadataService, typeTransformerRegistry);
    }

    private ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(participantContextId)
                .did("did")
                .apiTokenAlias("apiTokenAlias")
                .build();
    }

    private Holder createHolder(String id, String did, String name) {
        return Holder.Builder.newInstance()
                .participantContextId(UUID.randomUUID().toString())
                .holderId(id)
                .did(did)
                .holderName(name)
                .build();
    }

    private String generateJwt() {
        var ecKey = generateEcKey(null);
        var jwt = buildSignedJwt(new JWTClaimsSet.Builder().audience("test-audience")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issuer("test-issuer")
                .subject("test-subject")
                .jwtID(UUID.randomUUID().toString()).build(), ecKey);

        return "Bearer " + jwt.serialize();
    }
}
