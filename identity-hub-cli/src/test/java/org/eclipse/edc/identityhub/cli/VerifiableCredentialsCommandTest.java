/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.cli.CliTestUtils.PRIVATE_KEY_PATH;
import static org.eclipse.edc.identityhub.cli.CliTestUtils.toJwtVerifiableCredential;
import static org.eclipse.edc.identityhub.cli.CliTestUtils.verifyVerifiableCredentialSignature;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifiableCredentialsCommandTest {

    private static final ObjectMapper MAPPER = new TypeManager().getMapper();
    private static final Credential CREDENTIAL1 = VerifiableCredentialTestUtil.generateCredential();
    private static final JwtCredentialEnvelope VC1 = new JwtCredentialEnvelope(toJwtVerifiableCredential(CREDENTIAL1));
    private static final Credential CREDENTIAL2 = VerifiableCredentialTestUtil.generateCredential();
    private static final JwtCredentialEnvelope VC2 = new JwtCredentialEnvelope(toJwtVerifiableCredential(CREDENTIAL2));
    private static final String HUB_URL = "http://some.test.url";

    private final IdentityHubCli app = new IdentityHubCli();
    private final CommandLine cmd = new CommandLine(app);
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @BeforeEach
    void setUp() {
        app.identityHubClient = mock(IdentityHubClient.class);
        app.hubUrl = HUB_URL;
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
    }

    @Test
    void list() throws Exception {
        // arrange
        when(app.identityHubClient.getVerifiableCredentials(app.hubUrl)).thenReturn(Result.success(List.of(VC1, VC2)));

        // act
        var exitCode = executeList();
        var outContent = out.toString();
        var errContent = err.toString();

        // assert
        assertThat(exitCode).isZero();
        assertThat(errContent).isEmpty();

        var claims = MAPPER.readValue(outContent, new TypeReference<List<Map<String, Object>>>() {
        });
        var vcs = claims.stream()
                .map(c -> MAPPER.convertValue(c, Credential.class))
                .collect(Collectors.toList());

        assertThat(vcs)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(List.of(CREDENTIAL1, CREDENTIAL2));
    }

    @Test
    void add() throws Exception {
        // arrange
        var json = MAPPER.writeValueAsString(CREDENTIAL1.getCredentialSubject().getClaims());
        var vcArgCaptor = ArgumentCaptor.forClass(JwtCredentialEnvelope.class);
        doReturn(Result.success()).when(app.identityHubClient).addVerifiableCredential(eq(app.hubUrl), vcArgCaptor.capture());

        // act
        var exitCode = executeAdd(json, CREDENTIAL1.getIssuer(), CREDENTIAL1.getCredentialSubject().getId(), PRIVATE_KEY_PATH);
        var outContent = out.toString();
        var errContent = err.toString();

        // assert
        assertThat(exitCode).isZero();
        assertThat(outContent).isEqualTo("Verifiable Credential added successfully" + System.lineSeparator());
        assertThat(errContent).isEmpty();

        verify(app.identityHubClient).addVerifiableCredential(eq(app.hubUrl), isA(JwtCredentialEnvelope.class));
        var envelope = vcArgCaptor.getValue();
        var signedJwt = envelope.getJwt();

        // assert JWT signature
        assertThat(verifyVerifiableCredentialSignature(signedJwt)).isTrue();

        var result = envelope.toVerifiableCredential(MAPPER);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getItem()).usingRecursiveComparison()
                .ignoringFields("id")
                .ignoringFields("issuanceDate")
                .isEqualTo(CREDENTIAL1);
    }

    @Test
    void add_invalidJson_fails() {
        // arrange
        var json = "Invalid json";

        // act
        var exitCode = executeAdd(json, "issuer", "subject", PRIVATE_KEY_PATH);
        var outContent = out.toString();
        var errContent = err.toString();

        // assert
        assertThat(exitCode).isNotZero();
        assertThat(outContent).isEmpty();
        assertThat(errContent).contains("Error while processing request json");
    }

    @Test
    void add_invalidPrivateKey_fails() throws JsonProcessingException {
        // arrange
        var json = MAPPER.writeValueAsString(CREDENTIAL1.getCredentialSubject().getClaims());

        // act
        var exitCode = executeAdd(json, CREDENTIAL1.getIssuer(), CREDENTIAL2.getCredentialSubject().getId(), "non-existing-key");
        var outContent = out.toString();
        var errContent = err.toString();

        // assert
        assertThat(exitCode).isNotZero();
        assertThat(outContent).isEmpty();
        assertThat(errContent).contains("Error while signing Verifiable Credential");
    }

    private int executeList() {
        return cmd.execute("-s", HUB_URL, "vc", "list");
    }

    private int executeAdd(String json, String issuer, String subject, String privateKey) {
        return cmd.execute("-s", HUB_URL, "vc", "add", "-c", json, "-i", issuer, "-b", subject, "-k", privateKey);
    }
}
