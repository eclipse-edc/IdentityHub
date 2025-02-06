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

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.CredentialService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public class CredentialServiceImpl implements CredentialService {
    private final CredentialStore credentialStore;

    public CredentialServiceImpl(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public ServiceResult<Collection<VerifiableCredentialResource>> getForParticipant(String participantId) {
        var query = IdentityResource.queryByParticipantContextId(participantId).build();
        return query(query);
    }

    @Override
    public ServiceResult<Collection<VerifiableCredentialResource>> query(QuerySpec query) {
        return ServiceResult.from(credentialStore.query(query));
    }
}
