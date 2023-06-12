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

package org.eclipse.edc.identityhub.systemtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.cli.IdentityHubCli;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialSubject;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class VerifiableCredentialsIntegrationTest {

    private static final String HUB_URL = "http://localhost:8181/api/identity-hub";
    private static final String AUTHORITY_DID = "did:web:localhost%3A8080:authority";
    private static final String PARTICIPANT_DID = "did:web:localhost%3A8080:participant";
    private static final String AUTHORITY_PRIVATE_KEY_PATH = "resources/jwt/authority/private-key.pem";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Credential credential = createCredential();
    private ClientAndServer didServer;

    private final CommandLine cmd = IdentityHubCli.getCommandLine();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @BeforeEach
    void setUp(EdcExtension extension) {
        didServer = ClientAndServer.startClientAndServer(8080);
        didServer.when(request().withPath("/authority/did.json"))
                .respond(response(getResourceFileContentAsString("webdid/authority/did.json")));
        didServer.when(request().withPath("/participant/did.json"))
                .respond(response(getResourceFileContentAsString("webdid/participant/did.json")));
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));

        extension.setConfiguration(Map.of(
                "web.http.port", "8181",
                "web.http.path", "/api",
                "edc.iam.did.web.use.https", "false"));
    }

    @AfterEach
    public void stopServer() {
        stopQuietly(didServer);
    }

    @Test
    void push_and_get_verifiable_credentials(CredentialsVerifier verifier, DidResolverRegistry resolverRegistry) throws Exception {
        addVerifiableCredentialWithCli();

        var verifiedCredential = getVerifiedCredential(verifier, resolverRegistry);

        assertThat(verifiedCredential).isInstanceOf(Credential.class).usingRecursiveComparison()
                .ignoringFields("id", "issuanceDate")
                .isEqualTo(credential);
    }

    private void addVerifiableCredentialWithCli() throws JsonProcessingException {
        var json = MAPPER.writeValueAsString(credential.getCredentialSubject().getClaims());
        int result = cmd.execute("-s", HUB_URL, "vc", "add", "-c", json, "-i", credential.getIssuer(), "-b", credential.getCredentialSubject().getId(), "-k", AUTHORITY_PRIVATE_KEY_PATH);

        assertThat(result).isZero();
    }

    private Object getVerifiedCredential(CredentialsVerifier verifier, DidResolverRegistry resolverRegistry) {
        var didResult = resolverRegistry.resolve(PARTICIPANT_DID);
        assertThat(didResult.succeeded()).isTrue();

        var verifiedCredentials = verifier.getVerifiedCredentials(didResult.getContent());
        assertThat(verifiedCredentials.succeeded()).isTrue();

        var vcs = verifiedCredentials.getContent();
        assertThat(vcs).hasSize(1);
        return vcs.values().stream().findFirst()
                .orElseThrow(() -> new AssertionError("Failed to find verified credential"));
    }

    private Credential createCredential() {
        return Credential.Builder.newInstance()
                .context(VerifiableCredential.DEFAULT_CONTEXT)
                .id(UUID.randomUUID().toString())
                .type(VerifiableCredential.DEFAULT_TYPE)
                .issuer(AUTHORITY_DID)
                .issuanceDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(PARTICIPANT_DID)
                        .claim("hello", "world")
                        .claim("foo", "bar")
                        .build())
                .build();
    }
}
