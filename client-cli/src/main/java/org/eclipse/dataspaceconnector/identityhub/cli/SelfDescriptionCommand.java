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

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "sd", mixinStandardHelpOptions = true,
        description = "Manage Self-Description.",
        subcommands = {
                GetSelfDescriptionCommand.class,
        })
class SelfDescriptionCommand {
    @ParentCommand
    IdentityHubCli cli;
}
