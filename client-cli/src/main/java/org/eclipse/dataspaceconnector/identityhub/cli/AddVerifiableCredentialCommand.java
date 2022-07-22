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

package org.eclipse.dataspaceconnector.identityhub.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "add", description = "Adds a verifiable credential to identity hub")
class AddVerifiableCredentialCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParentCommand
    private VerifiableCredentialsCommand command;

    @CommandLine.Option(names = { "-c", "--verifiable-credential"}, required = true, description = "Verifiable Credential as JSON")
    private String verifiableCredentialJson;

    @CommandLine.Option(names = { "-i", "--issuer"}, required = true, description = "DID of the Verifiable Credential issuer")
    private String issuer;

    @CommandLine.Option(names = { "-k", "--private-key" }, required = true, description = "PEM file with private key for signing Verifiable Credentials")
    private String privateKeyPemFile;

    @Override
    public Integer call() throws Exception {
        VerifiableCredential vc;
        try {
            vc = MAPPER.readValue(verifiableCredentialJson, VerifiableCredential.class);
        } catch (JsonProcessingException e) {
            throw new CliException("Error while processing request json.");
        }

        var ecPrivateKey = JWTUtils.readECKey(new File(privateKeyPemFile)).toECPrivateKey();
        var signedJWT = JWTUtils.buildSignedJwt(vc, issuer, ecPrivateKey);

        command.cli.identityHubClient.addVerifiableCredential(command.cli.hubUrl, signedJWT);

        return 0;
    }


}
