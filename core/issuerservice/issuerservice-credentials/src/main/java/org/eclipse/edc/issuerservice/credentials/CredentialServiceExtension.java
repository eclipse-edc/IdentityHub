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

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.CredentialService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.issuerservice.statuslist.StatusListServiceExtension.NAME;

@Extension(value = NAME)
public class CredentialServiceExtension implements ServiceExtension {
    public static final String NAME = "Credential Service Extension";

    @Inject
    private CredentialStore store;

    @Provider
    public CredentialService getCredentialService() {
        return new CredentialServiceImpl(store);
    }
}
