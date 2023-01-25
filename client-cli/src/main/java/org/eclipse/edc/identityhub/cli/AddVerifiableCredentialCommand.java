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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialFactory;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialSubject;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.eclipse.edc.identityhub.cli.CryptoUtils.readEcKeyPemFile;


@Command(name = "add", description = "Adds a verifiable credential to identity hub")
class AddVerifiableCredentialCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JwtCredentialFactory JWT_CREDENTIAL_FACTORY = new JwtCredentialFactory(MAPPER);

    @ParentCommand
    private VerifiableCredentialsCommand command;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = { "-c", "--claims" }, required = true, description = "Claims of the Verifiable Credential")
    private String claims;

    @CommandLine.Option(names = { "-i", "--issuer" }, required = true, description = "DID of the Verifiable Credential issuer")
    private String issuer;

    @CommandLine.Option(names = { "-b", "--subject" }, required = true, description = "DID of the Verifiable Credential subject")
    private String subject;

    @CommandLine.Option(names = { "-k", "--private-key" }, required = true, description = "PEM file with EC private key for signing Verifiable Credentials")
    private String privateKeyPemFile;

    @Override
    public Integer call() throws Exception {
        var out = spec.commandLine().getOut();
        var credentialSubject = createCredentialSubject();
        var credential = toCredential(credentialSubject);
        var jwt = toJwt(credential);

        command.cli.identityHubClient.addVerifiableCredential(command.cli.hubUrl, new JwtCredentialEnvelope(jwt))
                .orElseThrow(responseFailure -> new CliException("Error while adding the Verifiable credential to the Identity Hub"));

        out.println("Verifiable Credential added successfully");

        return 0;
    }

    private CredentialSubject createCredentialSubject() {
        Map<String, Object> claimsMap;
        try {
            claimsMap = MAPPER.readValue(claims, Map.class);
        } catch (JsonProcessingException e) {
            throw new CliException("Error while processing request json.");
        }
        var builder = CredentialSubject.Builder.newInstance()
                .id(subject);
        claimsMap.forEach(builder::claim);
        return builder.build();
    }

    private Credential toCredential(CredentialSubject credentialSubject) {
        return Credential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuanceDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .context(VerifiableCredential.DEFAULT_CONTEXT)
                .type(VerifiableCredential.DEFAULT_TYPE)
                .credentialSubject(credentialSubject)
                .build();
    }

    private SignedJWT toJwt(Credential credential) {
        try {
            var privateKey = readEcKeyPemFile(privateKeyPemFile);
            return JWT_CREDENTIAL_FACTORY.buildSignedJwt(credential, new EcPrivateKeyWrapper(privateKey));
        } catch (Exception e) {
            throw new CliException("Error while signing Verifiable Credential", e);
        }
    }
}
