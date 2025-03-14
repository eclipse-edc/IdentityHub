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

package org.eclipse.edc.identityhub.protocols.dcp.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registry for {@link DcpProfile}.
 */
public interface DcpProfileRegistry {

    void registerProfile(DcpProfile profile);

    List<DcpProfile> profilesFor(CredentialFormat format);

    @Nullable
    DcpProfile getProfile(String name);
}
