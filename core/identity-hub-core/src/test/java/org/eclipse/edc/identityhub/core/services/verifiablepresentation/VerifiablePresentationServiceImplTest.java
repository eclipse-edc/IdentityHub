/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_LD;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.TestData.EMPTY_LDP_VP;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.TestData.JWT_VP;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.TestData.LDP_VP_WITH_PROOF;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.TestData.VCDM20_JWT_VP;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifiablePresentationServiceImplTest {

    private static final String TEST_AUDIENCE = "did:web:audience.com";
    private static final String TEST_PARTICIPANT_CONTEXT_ID = "test-participant";
    private final Monitor monitor = mock();
    private final PresentationCreatorRegistry registry = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private VerifiablePresentationServiceImpl presentationService;

    @Test
    void generate_noCredentials() {
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), anyList(), eq(VC1_0_LD), any())).thenReturn(jsonObject(EMPTY_LDP_VP));
        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);
        List<VerifiableCredentialContainer> ldpVcs = List.of();

        var result = presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, ldpVcs, null, null);
        assertThat(result).isSucceeded().matches(pr -> pr.getPresentation().isEmpty(), "VP Tokens should be empty");
    }

    @Test
    void generate_containsOnlyLdpVc() {
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_LD), any())).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);

        var credentials = List.of(createCredential(VC1_0_LD), createCredential(VC1_0_LD));
        var result = presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, credentials, null, null);

        assertThat(result).isSucceeded();
        verify(registry).createPresentation(
                eq(TEST_PARTICIPANT_CONTEXT_ID),
                argThat(argument -> argument.size() == 2),
                eq(VC1_0_LD),
                argThat(additional -> additional.get("types") instanceof List<?> typesList && typesList.contains("VerifiablePresentation")));
    }

    @Test
    void generate_mixedVcs() {
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_LD), any())).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT), any())).thenReturn(JWT_VP);
        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);

        var credentials = List.of(createCredential(VC1_0_LD), createCredential(VC1_0_JWT));

        var result = presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, credentials, null, null);
        assertThat(result).isSucceeded();
        assertThat(result.getContent().getPresentation()).hasSize(2);
        verify(registry).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), argThat(argument -> argument.size() == 1), eq(VC1_0_JWT), any());
        verify(registry).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), argThat(argument -> argument.size() == 1), eq(VC1_0_LD), any());
    }

    @Test
    void generate_onlyJwtVcs() {
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT), any())).thenReturn(JWT_VP);
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_LD), any())).thenReturn(jsonObject(EMPTY_LDP_VP));
        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);

        var credentials = List.of(createCredential(VC1_0_JWT), createCredential(VC1_0_JWT));

        var result = presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, credentials, null, TEST_AUDIENCE);
        assertThat(result).isSucceeded();
        assertThat(result.getContent().getPresentation()).hasSize(1);
        verify(registry).createPresentation(
                eq(TEST_PARTICIPANT_CONTEXT_ID),
                argThat(argument -> argument.size() == 2),
                eq(VC1_0_JWT),
                argThat(additional -> TEST_AUDIENCE.equals(additional.get(JwtRegisteredClaimNames.AUDIENCE)))
        );
        verify(registry, never()).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_LD), any());
    }

    @Test
    void generate_mixedVcs_withVcdm2JwtVcs() {
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_LD), any())).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT), any())).thenReturn(JWT_VP);
        when(registry.createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), any(), eq(VC2_0_JOSE), any())).thenReturn(VCDM20_JWT_VP);

        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);

        var credentials = List.of(createCredential(VC1_0_LD), createCredential(VC1_0_JWT), createCredential(VC2_0_JOSE));

        var result = presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, credentials, null, null);
        assertThat(result).isSucceeded();
        assertThat(result.getContent().getPresentation()).hasSize(3);
        verify(registry).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), argThat(argument -> argument.size() == 1), eq(VC1_0_JWT), any());
        verify(registry).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), argThat(argument -> argument.size() == 1), eq(VC1_0_LD), any());
        verify(registry).createPresentation(eq(TEST_PARTICIPANT_CONTEXT_ID), argThat(argument -> argument.size() == 1), eq(VC2_0_JOSE), any());
    }

    @Test
    void generate_withPresentationDef_shouldLogWarning() {
        presentationService = new VerifiablePresentationServiceImpl(registry, monitor);
        presentationService.createPresentation(TEST_PARTICIPANT_CONTEXT_ID, List.of(), PresentationDefinition.Builder.newInstance().id("test-id").build(), null);
        verify(monitor).warning(contains("A PresentationDefinition was submitted, but is currently ignored by the generator."));

    }

    protected VerifiableCredential createDummyCredential() {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test-subject")
                        .claim("test-claim", "test-value")
                        .build())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }

    private VerifiableCredentialContainer createCredential(CredentialFormat format) {
        return new VerifiableCredentialContainer("foobar", format, createDummyCredential());
    }

    private JsonObject jsonObject(String json) {
        try {
            return mapper.readValue(json, JsonObject.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}