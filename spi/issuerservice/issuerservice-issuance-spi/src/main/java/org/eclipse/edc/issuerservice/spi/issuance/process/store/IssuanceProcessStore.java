/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.issuance.process.store;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;

import java.util.stream.Stream;

/**
 * Stores {@link IssuanceProcess}.
 * <p>
 * There is at most one issuance process per holderPid and participant context: saving a new process whose holderPid is
 * used by another process of the same participant context fails with {@link StoreFailure.Reason#ALREADY_EXISTS}, also when
 * both are saved at the same time.
 */
@ExtensionPoint
public interface IssuanceProcessStore extends StateEntityStore<IssuanceProcess> {

    Stream<IssuanceProcess> query(QuerySpec querySpec);

    default String holderPidConflictMessage(IssuanceProcess process) {
        return "An issuance process with holderPid '%s' already exists for participant context '%s'."
                .formatted(process.getHolderPid(), process.getParticipantContextId());
    }
}
