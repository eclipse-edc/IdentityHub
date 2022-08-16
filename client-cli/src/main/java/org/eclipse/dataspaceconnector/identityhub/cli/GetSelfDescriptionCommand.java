/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;


@Command(name = "get", description = "Display Self-Description document.")
class GetSelfDescriptionCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @ParentCommand
    SelfDescriptionCommand command;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        var out = spec.commandLine().getOut();
        var result = command.cli.identityHubClient.getSelfDescription(command.cli.hubUrl);
        if (result.failed()) {
            throw new CliException("Error while getting Self-Description: " + result.getFailureDetail());
        }

        MAPPER.writeValue(out, result.getContent());
        out.println();
        return 0;
    }
}


