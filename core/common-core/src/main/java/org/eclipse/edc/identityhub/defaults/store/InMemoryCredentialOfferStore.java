/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults.store;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

public class InMemoryCredentialOfferStore extends InMemoryStatefulEntityStore<CredentialOffer> implements CredentialOfferStore {
    public InMemoryCredentialOfferStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryCredentialOfferStore(String leaserId, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(CredentialOffer.class, leaserId, clock, criterionOperatorRegistry, state -> CredentialOfferStatus.valueOf(state).code());
    }

    @Override
    public Collection<CredentialOffer> query(QuerySpec querySpec) {
        return findAll(querySpec).toList();
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        try {
            if (findById(id) == null) {
                return StoreResult.notFound("CredentialOffer with id '%s' not found".formatted(id));
            }
            delete(id);
            return StoreResult.success();
        } catch (IllegalStateException ex) {
            return StoreResult.alreadyLeased(ex.getMessage());
        }
    }
}
