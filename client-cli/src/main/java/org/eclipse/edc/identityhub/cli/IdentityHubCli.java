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

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.connector.core.base.EdcHttpClientImpl;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistryImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.types.TypeManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Command(name = "identity-hub-cli", mixinStandardHelpOptions = true,
        description = "Client utility for MVD identity hub.",
        subcommands = {
                VerifiableCredentialsCommand.class,
        })
public class IdentityHubCli {
    @CommandLine.Option(names = { "-s", "--identity-hub-url" }, required = true, description = "Identity Hub URL", defaultValue = "http://localhost:8181/api/identity-hub")
    String hubUrl;

    private static final int RETRIES = 3;
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int MIN_BACKOFF_MILLIS = 500;
    private static final int MAX_BACKOFF_MILLIS = 10000;

    IdentityHubClient identityHubClient;

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
        var typeManager = new TypeManager();
        var monitor = new ConsoleMonitor();

        var okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        var retryPolicy = RetryPolicy.<Response>builder()
                .withMaxRetries(RETRIES)
                .withBackoff(MIN_BACKOFF_MILLIS, MAX_BACKOFF_MILLIS, ChronoUnit.MILLIS)
                .build();

        var client = new EdcHttpClientImpl(okHttpClient, retryPolicy, monitor);

        var registry = new CredentialEnvelopeTransformerRegistryImpl();
        registry.register(new JwtCredentialEnvelopeTransformer(typeManager.getMapper()));

        identityHubClient = new IdentityHubClientImpl(client, typeManager, registry);
    }
}
