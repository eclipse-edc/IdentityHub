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

package org.eclipse.edc.issuerservice.credentials.statuslist;

import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactory;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;

import java.util.HashMap;
import java.util.Map;

public class StatusListInfoFactoryRegistryImpl implements StatusListInfoFactoryRegistry {
    private final Map<String, StatusListInfoFactory> registry = new HashMap<>();

    @Override
    public StatusListInfoFactory getInfoFactory(String type) {
        return registry.get(type);
    }

    @Override
    public void register(String type, StatusListInfoFactory factory) {
        registry.put(type, factory);
    }
}
