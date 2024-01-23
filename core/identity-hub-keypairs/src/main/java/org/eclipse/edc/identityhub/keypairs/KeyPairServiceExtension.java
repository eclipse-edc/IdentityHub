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

package org.eclipse.edc.identityhub.keypairs;

import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.identityhub.keypairs.KeyPairServiceExtension.NAME;

@Extension(NAME)
public class KeyPairServiceExtension implements ServiceExtension {
    public static final String NAME = "KeyPair Service Extension";

    @Inject
    private Vault vault;
    @Inject
    private KeyPairResourceStore keyPairResourceStore;

    @Provider
    public KeyPairService createParticipantService() {
        return new KeyPairServiceImpl(keyPairResourceStore, vault);
    }
}
