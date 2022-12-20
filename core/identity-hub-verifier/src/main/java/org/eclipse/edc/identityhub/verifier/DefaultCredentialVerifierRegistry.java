/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier;

import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialEnvelopeVerifier;
import org.eclipse.edc.identityhub.spi.credentials.verifier.CredentialVerifierRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation for {@link CredentialVerifierRegistry}
 */
public class DefaultCredentialVerifierRegistry implements CredentialVerifierRegistry {

    private final Map<String, CredentialEnvelopeVerifier> registry = new HashMap<>();

    @Override
    public void register(String format, CredentialEnvelopeVerifier verifier) {
        registry.put(format, verifier);
    }

    @Override
    @Nullable
    public CredentialEnvelopeVerifier resolve(String format) {
        return registry.get(format);
    }
}
