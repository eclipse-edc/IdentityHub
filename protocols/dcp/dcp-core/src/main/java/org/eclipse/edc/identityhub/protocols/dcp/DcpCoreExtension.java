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
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.identityhub.protocols.dcp.DcpCoreExtension.NAME;

@Extension(NAME)
public class DcpCoreExtension implements ServiceExtension {

    public static final String NAME = "DCP Core Extension";

    public static final String VC_20_BSSL_JWT = "vc20-bssl/jwt";
    public static final String VC_11_SL_2021_JWT = "vc11-sl2021/jwt";
    private DcpProfileRegistry registry;

    @Provider
    public DcpProfileRegistry dcpProfileRegistry() {

        if (registry == null) {
            registry = new DcpProfileRegistryImpl();
        }
        // Register DCP profiles https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/HEAD/#profiles-of-the-decentralized-claims-protocol
        registry.registerProfile(new DcpProfile(VC_20_BSSL_JWT, CredentialFormat.VC2_0_JOSE, BitstringStatusListStatus.TYPE));
        registry.registerProfile(new DcpProfile(VC_11_SL_2021_JWT, CredentialFormat.VC1_0_JWT, StatusList2021Status.TYPE));
        return registry;
    }


}
