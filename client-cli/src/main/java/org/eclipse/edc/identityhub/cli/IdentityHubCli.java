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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistryImpl;
import org.eclipse.edc.identityhub.verifier.jwt.VerifiableCredentialsJwtService;
import org.eclipse.edc.identityhub.verifier.jwt.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "identity-hub-cli", mixinStandardHelpOptions = true,
        description = "Client utility for MVD identity hub.",
        subcommands = {
                VerifiableCredentialsCommand.class,
                SelfDescriptionCommand.class
        })
public class IdentityHubCli {
    @CommandLine.Option(names = { "-s", "--identity-hub-url" }, required = true, description = "Identity Hub URL", defaultValue = "http://localhost:8181/api/identity-hub")
    String hubUrl;

    IdentityHubClient identityHubClient;

    VerifiableCredentialsJwtService verifiableCredentialsJwtService;

    public static void main(String... args) {
        CommandLine commandLine = getCommandLine();
        var exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    public static CommandLine getCommandLine() {
        var command = new IdentityHubCli();
        return new CommandLine(command).setExecutionStrategy(command::executionStrategy);
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        init(); // custom initialization to be done before executing any command or subcommand
        return new CommandLine.RunLast().execute(parseResult);
    }

    private void init() {
        var okHttpClient = new OkHttpClient.Builder().build();
        var objectMapper = new ObjectMapper();
        var monitor = new ConsoleMonitor();

        var registry = new CredentialEnvelopeTransformerRegistryImpl();
        registry.register(new JwtCredentialEnvelopeTransformer(objectMapper));

        identityHubClient = new IdentityHubClientImpl(okHttpClient, objectMapper, monitor, registry);
        verifiableCredentialsJwtService = new VerifiableCredentialsJwtServiceImpl(objectMapper, monitor);
    }
}
