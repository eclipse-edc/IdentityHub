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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collections;
import java.util.List;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.NOT_YET_VALID;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.SUSPENDED;

/**
 * This is a runnable task that is intended to be executed periodically to fetch all non-expired, non-revoked credentials from storage, check for their status,
 * and update their status. Every execution (fetch-all - check-each - update-each) will run in a transaction.
 * <p>
 * Note that this will materialize <strong>all</strong> credentials into memory at once, as the general assumption is that typically, wallets don't
 * store an enormous amount of credentials.
 * <p>
 * Note also, that a credential's status will only be updated if it did in fact change, to avoid unnecessary database interactions.
 */
public class CredentialWatchdog implements Runnable {
    //todo: add more states once we have to check issuance status
    public static final List<Integer> ALLOWED_STATES = List.of(ISSUED.code(), NOT_YET_VALID.code(), SUSPENDED.code());
    private final CredentialStore credentialStore;
    private final CredentialStatusCheckService credentialStatusCheckService;
    private final Monitor monitor;
    private final TransactionContext transactionContext;

    public CredentialWatchdog(CredentialStore credentialStore, CredentialStatusCheckService credentialStatusCheckService, Monitor monitor, TransactionContext transactionContext) {
        this.credentialStore = credentialStore;
        this.credentialStatusCheckService = credentialStatusCheckService;
        this.monitor = monitor;
        this.transactionContext = transactionContext;
    }

    @Override
    public void run() {
        transactionContext.execute(() -> {
            var allCredentials = credentialStore.query(allExcludingExpiredAndRevoked())
                    .onFailure(f -> monitor.warning("Failed to fetch credentials from database: %s".formatted(f.getFailureDetail())))
                    .orElse(f -> Collections.emptyList());

            monitor.debug("checking %d credentials".formatted(allCredentials.size()));

            allCredentials.forEach(credential -> {
                var status = credentialStatusCheckService.checkStatus(credential)
                        .orElse(f -> {
                            monitor.warning("Error determining status for credential '%s': %s. Will move to the ERROR state.".formatted(credential.getId(), f.getFailureDetail()));
                            return VcStatus.ERROR;
                        });
                var changed = credential.getState() != status.code();
                if (changed) {
                    credential.setCredentialStatus(status);
                    credentialStore.update(credential);
                }
            });
        });
    }

    private QuerySpec allExcludingExpiredAndRevoked() {
        return QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "in", ALLOWED_STATES))
                .build();
    }
}
