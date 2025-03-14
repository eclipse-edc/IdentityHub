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

package org.eclipse.edc.identityhub.protocols.dcp;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DcpProfileRegistryImpl implements DcpProfileRegistry {
    private final Map<String, DcpProfile> profiles = new HashMap<>();

    @Override
    public void registerProfile(DcpProfile profile) {
        profiles.put(profile.name(), profile);
    }

    @Override
    public List<DcpProfile> profilesFor(CredentialFormat format) {
        return profiles.values().stream()
                .filter(dcpProfile -> dcpProfile.format().equals(format))
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable DcpProfile getProfile(String name) {
        return profiles.get(name);
    }
}
