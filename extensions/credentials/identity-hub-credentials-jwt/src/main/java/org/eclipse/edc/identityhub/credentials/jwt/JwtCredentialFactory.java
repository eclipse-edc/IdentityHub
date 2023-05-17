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

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;

import java.text.ParseException;
import java.util.Map;

import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_CREDENTIALS_KEY;

public class JwtCredentialFactory {

    private final ObjectMapper mapper;

    public JwtCredentialFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Build Verifiable Credential in JWT format such as defined in <a href="https://www.w3.org/TR/vc-data-model/#example-usage-of-the-id-property">W3C specification</a>.
     * The credential is defined in the `vc` claim of the token, such as:
     * <pre>{@code
     * "credentialId" : {
     *     "vc": {
     *         "credentialSubject": {
     *             // some claims about the subject.
     *         }
     *     }
     *     "iss": "issuer-value",
     *     "sub": "subject-value",
     *     // other JWT claims
     * }
     * }</pre>
     *
     * @param credential the credential
     * @param privateKey private key for signing the JWT
     * @return the verifiable credential in JWT format.
     */
    public SignedJWT buildSignedJwt(Credential credential, PrivateKeyWrapper privateKey) throws JOSEException, ParseException {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        // this step of preparatory mapping is required as nimbus relies on Gson which will not respect the format annotation defined in the @Credential object.
        var mapped = mapper.convertValue(credential, Map.class);
        var claims = new JWTClaimsSet.Builder()
                .claim(VERIFIABLE_CREDENTIALS_KEY, mapped)
                .issuer(credential.getIssuer())
                .issueTime(credential.getIssuanceDate())
                .expirationTime(credential.getExpirationDate())
                .subject(credential.getCredentialSubject().getId())
                .build();

        var jws = new SignedJWT(jwsHeader, claims);

        jws.sign(privateKey.signer());

        return SignedJWT.parse(jws.serialize());
    }
}
