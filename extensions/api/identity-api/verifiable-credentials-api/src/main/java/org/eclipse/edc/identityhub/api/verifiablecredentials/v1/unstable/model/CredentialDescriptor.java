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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;

public record CredentialDescriptor(String format, String type, String id) {
    public CredentialDescriptor(CredentialFormat format, String type, String id) {
        this(format.name(), type, id);
    }
}
