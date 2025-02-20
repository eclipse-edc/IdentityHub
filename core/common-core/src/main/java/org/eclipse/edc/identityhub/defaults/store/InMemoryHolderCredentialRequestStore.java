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

package org.eclipse.edc.identityhub.defaults.store;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

public class InMemoryHolderCredentialRequestStore extends InMemoryStatefulEntityStore<HolderCredentialRequest> implements HolderCredentialRequestStore {
    public InMemoryHolderCredentialRequestStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryHolderCredentialRequestStore(String leaserId, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(HolderCredentialRequest.class, leaserId, clock, criterionOperatorRegistry, state -> HolderRequestState.valueOf(state).code());
    }

    @Override
    public Collection<HolderCredentialRequest> query(QuerySpec query) {
        return super.findAll(query).toList();
    }
}
