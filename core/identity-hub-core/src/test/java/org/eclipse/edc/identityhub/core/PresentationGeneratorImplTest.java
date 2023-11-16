/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.identitytrust.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.identityhub.core.creators.TestData.EMPTY_LDP_VP;
import static org.eclipse.edc.identityhub.core.creators.TestData.JWT_VP;
import static org.eclipse.edc.identityhub.core.creators.TestData.LDP_VP_WITH_PROOF;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JSON_LD;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JWT;
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

class PresentationGeneratorImplTest {

    private final Monitor monitor = mock();
    private final PresentationCreatorRegistry registry = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private PresentationGeneratorImpl presentationGenerator;


    @Test
    void generate_noCredentials() {
        when(registry.createPresentation(anyList(), eq(JSON_LD))).thenReturn(jsonObject(EMPTY_LDP_VP));
        presentationGenerator = new PresentationGeneratorImpl(JSON_LD, registry, monitor);
        List<VerifiableCredentialContainer> ldpVcs = List.of();

        var result = presentationGenerator.createPresentation(ldpVcs, null);
        assertThat(result).isSucceeded();
    }

    @Test
    void generate_defaultFormatLdp_containsOnlyLdpVc() {
        when(registry.createPresentation(any(), eq(JSON_LD))).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        presentationGenerator = new PresentationGeneratorImpl(JSON_LD, registry, monitor);

        var credentials = List.of(createCredential(JSON_LD), createCredential(JSON_LD));
        var result = presentationGenerator.createPresentation(credentials, null);

        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 2), eq(JSON_LD));
    }

    @Test
    void generate_defaultFormatLdp_mixedVcs() {
        when(registry.createPresentation(any(), eq(JSON_LD))).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        when(registry.createPresentation(any(), eq(JWT))).thenReturn(JWT_VP);
        presentationGenerator = new PresentationGeneratorImpl(JSON_LD, registry, monitor);

        var credentials = List.of(createCredential(JSON_LD), createCredential(JWT));

        var result = presentationGenerator.createPresentation(credentials, null);
        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 1), eq(JWT));
        verify(registry).createPresentation(argThat(argument -> argument.size() == 1), eq(JSON_LD));
        verify(monitor).warning(eq("The VP was requested in JSON_LD format, but the request yielded 1 JWT-VCs, which cannot be transported in a LDP-VP. A second VP will be returned, containing JWT-VCs"));
    }

    @Test
    void generate_defaultFormatLdp_onlyJwtVcs() {
        when(registry.createPresentation(any(), eq(JWT))).thenReturn(JWT_VP);
        when(registry.createPresentation(any(), eq(JSON_LD))).thenReturn(jsonObject(EMPTY_LDP_VP));
        presentationGenerator = new PresentationGeneratorImpl(JSON_LD, registry, monitor);

        var credentials = List.of(createCredential(JWT), createCredential(JWT));

        var result = presentationGenerator.createPresentation(credentials, null);
        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 2), eq(JWT));
        verify(registry, never()).createPresentation(any(), eq(JSON_LD));
        verify(monitor).warning(eq("The VP was requested in JSON_LD format, but the request yielded 2 JWT-VCs, which cannot be transported in a LDP-VP. A second VP will be returned, containing JWT-VCs"));
    }

    @Test
    void generate_defaultFormatJwt_onlyJwtVcs() {
        when(registry.createPresentation(any(), eq(JWT))).thenReturn(JWT_VP);
        when(registry.createPresentation(any(), eq(JSON_LD))).thenReturn(jsonObject(EMPTY_LDP_VP));
        presentationGenerator = new PresentationGeneratorImpl(JWT, registry, monitor);

        var credentials = List.of(createCredential(JWT), createCredential(JWT));

        var result = presentationGenerator.createPresentation(credentials, null);
        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 2), eq(JWT));
        verify(registry, never()).createPresentation(any(), eq(JSON_LD));
    }

    @Test
    void generate_defaultFormatJwt_mixedVcs() {
        when(registry.createPresentation(any(), eq(JSON_LD))).thenReturn(jsonObject(LDP_VP_WITH_PROOF));
        when(registry.createPresentation(any(), eq(JWT))).thenReturn(JWT_VP);
        presentationGenerator = new PresentationGeneratorImpl(JWT, registry, monitor);

        var credentials = List.of(createCredential(JSON_LD), createCredential(JWT));

        var result = presentationGenerator.createPresentation(credentials, null);
        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 2), eq(JWT));
        verify(registry, never()).createPresentation(any(), eq(JSON_LD));
    }

    @Test
    void generate_defaultFormatJwt_onlyLdpVc() {
        when(registry.createPresentation(any(), eq(JWT))).thenReturn(JWT_VP);
        presentationGenerator = new PresentationGeneratorImpl(JWT, registry, monitor);

        var credentials = List.of(createCredential(JSON_LD), createCredential(JSON_LD));
        var result = presentationGenerator.createPresentation(credentials, null);

        assertThat(result).isSucceeded();
        verify(registry).createPresentation(argThat(argument -> argument.size() == 2), eq(JWT));
        verify(registry, never()).createPresentation(any(), eq(JSON_LD));
    }

    @Test
    void generate_withPresentationDef_shouldLogWarning() {
        presentationGenerator = new PresentationGeneratorImpl(JSON_LD, registry, monitor);
        presentationGenerator.createPresentation(List.of(), PresentationDefinition.Builder.newInstance().id("test-id").build());
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