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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nimbusds.jwt.SignedJWT;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.toList;

@Command(name = "list", description = "Lists verifiable credentials")
class ListVerifiableCredentialsCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @ParentCommand
    private VerifiableCredentialsCommand command;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        var out = spec.commandLine().getOut();
        var result = command.cli.identityHubClient.getVerifiableCredentials(command.cli.hubUrl);
        if (result.failed()) {
            throw new CliException("Failed to get verifiable credentials: " + result.getFailureDetail());
        }
        var vcs = result.getContent().stream()
                .map(this::getClaims)
                .collect(toList());
        MAPPER.writeValue(out, vcs);
        out.println();
        return 0;
    }

    private Map<String, Object> getClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new CliException("Error while reading Verifiable Credentials claims", e);
        }
    }
}
