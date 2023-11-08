/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

public class CredentialQueryResolverImpl implements CredentialQueryResolver {

    private final CredentialStore credentialStore;

    public CredentialQueryResolverImpl(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public Result<List<VerifiableCredentialContainer>> query(PresentationQuery query, List<String> issuerScopes) {
        return null;
    }
}
