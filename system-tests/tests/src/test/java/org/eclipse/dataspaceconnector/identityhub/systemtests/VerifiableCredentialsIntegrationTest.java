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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import net.minidev.json.JSONObject;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.cli.IdentityHubCli;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.identityhub.verifier.IdentityHubCredentialsVerifier;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class VerifiableCredentialsIntegrationTest {

    static final Faker FAKER = new Faker();
    static final String HUB_URL = "http://localhost:8182/api/identity-hub";
    static final String AUTHORITY_DID = "did:web:localhost:authority";
    static final String PARTICIPANT_DID = "did:web:localhost:participant";
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final VerifiableCredential VC1 = VerifiableCredential.Builder.newInstance()
            .id(FAKER.internet().uuid())
            .credentialSubject(Map.of(
                    FAKER.internet().uuid(), FAKER.lorem().word(),
                    FAKER.internet().uuid(), FAKER.lorem().word()))
            .build();

    IdentityHubCredentialsVerifier verifier;

    IdentityHubCli app = new IdentityHubCli();
    CommandLine cmd = IdentityHubCli.getCommandLine();
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

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
        int result = cmd.execute("-s", HUB_URL, "vc", "list");
        assertThat(result).isEqualTo(0);

        var claims = MAPPER.readValue(out.toString(), new TypeReference<List<Map<String, Object>>>() {});
        //assertThat(claims).describedAs("Identity Hub already contains Verifiable Credentials").isEmpty();

        var json = MAPPER.writeValueAsString(VC1);
        cmd.execute("-s", HUB_URL, "vc", "add", "-c", json, "-i", AUTHORITY_DID, "-b", PARTICIPANT_DID, "-k", "resources/jwt/authority/private-key.pem");

        var didResult = resolverRegistry.resolve("did:web:localhost:participant");
        assertThat(didResult.succeeded()).isTrue();

        Result<Map<String, Object>> verifiedCredentials = verifier.getVerifiedCredentials(didResult.getContent());
        assertThat(verifiedCredentials.succeeded()).isTrue();
        Map<String, Object> vcs = verifiedCredentials.getContent();
        assertThat(vcs).containsKey(VC1.getId());

        Map<String, JSONObject> vc = (Map<String, JSONObject>) vcs.get(VC1.getId());
        VerifiableCredential vc1 = MAPPER.convertValue(vc.get("vc"), VerifiableCredential.class);
        assertThat(vc1).usingRecursiveComparison().isEqualTo(VC1);
    }
}