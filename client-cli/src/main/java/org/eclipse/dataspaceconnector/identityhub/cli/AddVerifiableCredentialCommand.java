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
import org.eclipse.dataspaceconnector.identityhub.model.credentials.VerifiableCredential;
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
    private String requestJson;

    @CommandLine.Option(names = { "-p", "--private-key" }, required = true, description = "PEM file with private key for signing Verifiable Credentials")
    private String privateKeyPemFile;

    @Override
    public Integer call() throws Exception {
        VerifiableCredential vc = null;
        try {
            vc = MAPPER.readValue(requestJson, VerifiableCredential.class);
        } catch (JsonProcessingException e) {
            throw new CliException("Error while processing request json.");
        }

        var ecPrivateKey = JWTUtils.readECPrivateKey(new File(privateKeyPemFile));
        var signedJWT = JWTUtils.buildSignedJwt(vc, "identity-hub-cli", ecPrivateKey);

        command.cli.identityHubClient.addVerifiableCredential(command.cli.hubUrl, signedJWT);

        return 0;
    }


}
