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

package org.eclipse.edc.issuerservice.spi.credentials;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.jetbrains.annotations.Nullable;

public record CredentialDescriptor(String format, String credentialType, @Nullable String reason) {
    public CredentialDescriptor(CredentialFormat format, String credentialType) {
        this(format.name(), credentialType, null);
    }
}
