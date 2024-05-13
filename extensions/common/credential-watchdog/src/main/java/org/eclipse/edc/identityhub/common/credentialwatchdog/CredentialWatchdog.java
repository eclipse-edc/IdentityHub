/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.common.credentialwatchdog;

import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Collections;

public class CredentialWatchdog implements Runnable {
    private final CredentialStore credentialStore;
    private final CredentialStatusCheckService credentialStatusCheckService;
    private final Monitor monitor;

    public CredentialWatchdog(CredentialStore credentialStore, CredentialStatusCheckService credentialStatusCheckService, Monitor monitor) {
        this.credentialStore = credentialStore;
        this.credentialStatusCheckService = credentialStatusCheckService;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        var allCredentials = credentialStore.query(QuerySpec.max())
                .onFailure(f -> monitor.warning("Failed to fetch credentials from database: %s".formatted(f.getFailureDetail())))
                .orElse(f -> Collections.emptyList());

        monitor.debug("checking %d credentials".formatted(allCredentials.size()));

        allCredentials.forEach(credential -> {
            var status = credentialStatusCheckService.checkStatus(credential)
                    .orElse(f -> {
                        monitor.warning("Error determining status for credential '%s': %s. Will move to the ERROR state.".formatted(credential.getId(), f.getFailureDetail()));
                        return VcStatus.ERROR;
                    });
            credential.setCredentialStatus(status);
            credentialStore.update(credential);
        });
    }
}
