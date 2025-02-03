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

package org.eclipse.edc.issuerservice.statuslist;

import org.eclipse.edc.issuerservice.spi.statuslist.StatusListCredentialFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListInfoFactory;

import java.util.HashMap;
import java.util.Map;

public class StatusListCredentialFactoryRegistryImpl implements StatusListCredentialFactoryRegistry {
    private final Map<String, StatusListInfoFactory> registry = new HashMap<>();

    @Override
    public StatusListInfoFactory getStatusListCredential(String type) {
        return registry.get(type);
    }

    @Override
    public void register(String type, StatusListInfoFactory factory) {
        registry.put(type, factory);
    }
}
