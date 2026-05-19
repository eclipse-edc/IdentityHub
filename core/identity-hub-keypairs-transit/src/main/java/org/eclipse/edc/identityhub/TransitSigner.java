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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import org.eclipse.edc.identityhub.transit.TransitEngine;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;


/**
 * A JSON Web Signature (JWS) signer that integrates with the HashiCorp Vault Transit secret engine
 * to perform signing operations. This class utilizes a specified Transit key to compute digital signatures
 * for signing input data.
 * <p>
 * This implementation supports multiple JWS algorithms, as defined by the Transit secret engine,
 * including EdDSA, EC, and RSA families. The signer delegates signing operations to the
 * {@code TransitEngine} for secure processing.
 */
public class TransitSigner implements JWSSigner {
    private final JCAContext jcaContext = new JCAContext();
    private final TransitEngine transitEngine;
    private final String keyName;

    public TransitSigner(TransitEngine transitEngine, String keyName) {
        this.transitEngine = transitEngine;
        this.keyName = keyName;
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        // signingInput is ASCII(BASE64URL(header) || '.' || BASE64URL(payload))
        var payload = new String(signingInput, StandardCharsets.US_ASCII);
        var result = transitEngine.sign(keyName, payload);
        if (result.failed()) {
            throw new JOSEException("Transit signing failed: " + result.getFailureDetail());
        }
        // Vault returns the signature as "vault:v<version>:<base64-encoded-signature>"
        var parts = result.getContent().split(":");
        if (parts.length < 3) {
            throw new JOSEException("Unexpected Transit signature format: " + result.getContent());
        }
        var sigBytes = Base64.getDecoder().decode(parts[2]);
        return Base64URL.encode(sigBytes);
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        // these are the algorithms supported by the Transit engine: EdDSA, EC and RSA
        var algorithms = JWSAlgorithm.Family.SIGNATURE.toArray(new JWSAlgorithm[]{});
        return Set.of(algorithms);
    }

    @Override
    public JCAContext getJCAContext() {
        return jcaContext;
    }
}
