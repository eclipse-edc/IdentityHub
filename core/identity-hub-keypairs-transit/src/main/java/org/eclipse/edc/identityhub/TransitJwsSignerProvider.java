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

import com.nimbusds.jose.JWSSigner;
import org.eclipse.edc.identityhub.transit.TransitEngine;
import org.eclipse.edc.jwt.spi.signer.JwsSignerProvider;
import org.eclipse.edc.spi.result.Result;


/**
 * A provider for JSON Web Signature (JWS) signers that integrates with
 * the HashiCorp Vault Transit secret engine. This class facilitates the creation
 * of {@code JWSSigner} instances using keys managed by the Transit secret engine.
 * <p>
 * The {@code TransitJwsSignerProvider} always returns a {@link TransitSigner} instance,
 * delegating the actual signing operations to the underlying {@code TransitEngine}.
 */
public class TransitJwsSignerProvider implements JwsSignerProvider {
    private final TransitEngine transitEngine;

    public TransitJwsSignerProvider(TransitEngine transitEngine) {
        this.transitEngine = transitEngine;
    }

    @Override
    public Result<JWSSigner> createJwsSigner(String privateKeyId) {
        return createJwsSigner(null, privateKeyId);
    }

    @Override
    public Result<JWSSigner> createJwsSigner(String participantContextId, String privateKeyName) {
        return Result.success(new TransitSigner(transitEngine, privateKeyName));
    }
}
