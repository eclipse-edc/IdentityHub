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

package org.eclipse.dataspaceconnector.identityhub.systemtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import net.minidev.json.JSONObject;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.cli.IdentityHubCli;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService.VERIFIABLE_CREDENTIALS_KEY;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class VerifiableCredentialsIntegrationTest {

    private static final Faker FAKER = new Faker();
    private static final String HUB_URL = "http://localhost:8182/api/identity-hub";
    private static final String AUTHORITY_DID = "did:web:localhost%3A8080:authority";
    private static final String PARTICIPANT_DID = "did:web:localhost%3A8080:participant";
    private static final String AUTHORITY_PRIVATE_KEY_PATH = "resources/jwt/authority/private-key.pem";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final VerifiableCredential VC1 = VerifiableCredential.Builder.newInstance()
            .id(FAKER.internet().uuid())
            .credentialSubject(Map.of(
                    FAKER.internet().uuid(), FAKER.lorem().word(),
                    FAKER.internet().uuid(), FAKER.lorem().word()))
            .build();

    private final CommandLine cmd = IdentityHubCli.getCommandLine();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @BeforeEach
    void setUp(EdcExtension extension) {
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));

        extension.setConfiguration(Map.of(
                "edc.identity.hub.url", HUB_URL,
                "edc.iam.did.web.use.https", "false"));
    }

    @Test
    void push_and_get_verifiable_credentials(CredentialsVerifier verifier, DidResolverRegistry resolverRegistry) throws Exception {
        addVerifiableCredentialWithCli();
        assertGetVerifiedCredentials(verifier, resolverRegistry);
    }

    @Test
    void get_self_description() {
        int result = cmd.execute("-s", HUB_URL, "sd", "get");
        assertThat(result).isZero();
        assertThat(out.toString()).contains("did:web:test.delta-dao.com");
    }

    private void addVerifiableCredentialWithCli() throws JsonProcessingException {
        var json = MAPPER.writeValueAsString(VC1);
        int result = cmd.execute("-s", HUB_URL, "vc", "add", "-c", json, "-i", AUTHORITY_DID, "-b", PARTICIPANT_DID, "-k", AUTHORITY_PRIVATE_KEY_PATH);
        assertThat(result).isZero();
    }

    private void assertGetVerifiedCredentials(CredentialsVerifier verifier, DidResolverRegistry resolverRegistry) {
        var didResult = resolverRegistry.resolve(PARTICIPANT_DID);
        assertThat(didResult.succeeded()).isTrue();

        var verifiedCredentials = verifier.getVerifiedCredentials(didResult.getContent());
        assertThat(verifiedCredentials.succeeded()).isTrue();

        var vcs = verifiedCredentials.getContent();
        assertThat(vcs)
                .extractingByKey(VC1.getId())
                .asInstanceOf(map(String.class, JSONObject.class))
                .extractingByKey(VERIFIABLE_CREDENTIALS_KEY)
                .satisfies(c -> {
                    assertThat(MAPPER.convertValue(c, VerifiableCredential.class))
                            .usingRecursiveComparison()
                            .isEqualTo(VC1);
                });
    }
}