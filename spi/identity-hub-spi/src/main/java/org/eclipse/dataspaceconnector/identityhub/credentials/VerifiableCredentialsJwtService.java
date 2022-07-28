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

package org.eclipse.dataspaceconnector.identityhub.credentials;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;

/**
 * Service with operations for manipulation of VerifiableCredentials in JWT format.
 */
public interface VerifiableCredentialsJwtService {

    String VERIFIABLE_CREDENTIALS_KEY = "vc";

    /**
     * Builds a verifiable credential as a signed JWT
     *
     * @param credential The verifiable credential to sign
     * @param issuer     The issuer of the verifiable credential
     * @param subject    The subject of the verifiable credential
     * @param privateKey The private key of the issuer, used for signing
     * @return The Verifiable Credential as a JWT
     * @throws Exception In case the credential can not be signed
     */
    SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, String subject, PrivateKeyWrapper privateKey) throws Exception;

    /**
     * Extract verifiable credentials from a JWT. The credential is represented with the following format
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
     * The representation is used to support any type of <a href="https://www.w3.org/TR/vc-data-model">verifiable credentials</a> .
     * When applying policies, the policy engine might need to access the issuer of the claim. That's why the JWT claims are included.
     *
     * @param jwt SignedJWT containing a verifiableCredential in its payload.
     * @return VerifiableCredential represented as {@code Map.Entry<String, Object>}.
     */
    Result<Map.Entry<String, Object>> extractCredential(SignedJWT jwt);

}
