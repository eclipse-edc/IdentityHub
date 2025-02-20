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

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

public class InMemoryIssuanceProcessStore extends InMemoryStatefulEntityStore<IssuanceProcess> implements IssuanceProcessStore {

    public InMemoryIssuanceProcessStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryIssuanceProcessStore(String leaserId, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(IssuanceProcess.class, leaserId, clock, criterionOperatorRegistry, state -> IssuanceProcessStates.valueOf(state).code());
    }

    @Override
    public Stream<IssuanceProcess> query(QuerySpec querySpec) {
        return super.findAll(querySpec);
    }
}
