/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer.spi;

import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;

/**
 * The <a href="https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/HEAD/#profiles-of-the-decentralized-claims-protocol">DCP profile</a>
 * of the issuer, as configured. It determines how the status of the issued credentials is presented, i.e. the kind of
 * status list and its format, e.g. a VCDM 1.1 StatusList2021 credential ({@code vc11-sl2021/jwt}) or a VCDM 2.0 Bitstring
 * Status List credential ({@code vc20-bssl/jwt}). Credentials are issued in the format of their credential definition,
 * which may be another one than that of the profile.
 * <p>
 * The runtime does not start with a profile that is not registered, or that it cannot handle. Other profiles than those
 * of DCP can be supported by extensions, which register the profile, a credential generator for its format and a status
 * list manager for its status list type.
 */
@FunctionalInterface
public interface DcpIssuerProfile {

    /**
     * The profile of the issuer.
     */
    DcpProfile get();
}
