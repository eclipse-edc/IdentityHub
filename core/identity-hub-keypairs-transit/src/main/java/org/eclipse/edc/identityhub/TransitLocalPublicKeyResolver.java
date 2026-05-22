/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub;

import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.spi.result.Result;

import java.security.PublicKey;

public class TransitLocalPublicKeyResolver implements LocalPublicKeyService {
    @Override
    public Result<PublicKey> resolveKey(String id) {
        throw new UnsupportedOperationException("Resolving keys with this service is not supported.");
    }
}
