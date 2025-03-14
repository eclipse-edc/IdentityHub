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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DcpProfileRegistryImplTest {

    private final DcpProfileRegistry registry = new DcpProfileRegistryImpl();


    @Test
    void register() {
        var profile = new DcpProfile("profile", CredentialFormat.VC1_0_JWT, "type");
        registry.registerProfile(profile);

        assertThat(registry.getProfile("profile")).isEqualTo(profile);
    }

    @Test
    void profilesFor() {
        var profile = new DcpProfile("profile", CredentialFormat.VC1_0_JWT, "type");
        var profile1 = new DcpProfile("profile1", CredentialFormat.VC1_0_JWT, "type2");

        registry.registerProfile(profile);
        registry.registerProfile(profile1);

        assertThat(registry.profilesFor(CredentialFormat.VC1_0_JWT)).containsOnly(profile, profile1);
    }
}
