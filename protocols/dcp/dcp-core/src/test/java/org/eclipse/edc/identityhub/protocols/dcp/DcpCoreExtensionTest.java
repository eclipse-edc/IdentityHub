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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.protocols.dcp.DcpCoreExtension.VC_11_SL_2021_JWT;
import static org.eclipse.edc.identityhub.protocols.dcp.DcpCoreExtension.VC_20_BSSL_JWT;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpCoreExtensionTest {
    

    @Test
    void initialize_verifyTokenRules(DcpCoreExtension extension, ServiceExtensionContext context) {

        assertThat(extension.dcpProfileRegistry()).isInstanceOf(DcpProfileRegistryImpl.class)
                .satisfies(registry -> {
                    assertThat(registry.getProfile(VC_20_BSSL_JWT)).isEqualTo(new DcpProfile(VC_20_BSSL_JWT, CredentialFormat.VC2_0_JOSE, BitstringStatusListStatus.TYPE));
                    assertThat(registry.getProfile(VC_11_SL_2021_JWT)).isEqualTo(new DcpProfile(VC_11_SL_2021_JWT, CredentialFormat.VC1_0_JWT, StatusList2021Status.TYPE));
                });

    }
}
