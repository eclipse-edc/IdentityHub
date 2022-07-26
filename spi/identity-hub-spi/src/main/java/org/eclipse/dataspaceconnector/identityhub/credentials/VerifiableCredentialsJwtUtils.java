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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class VerifiableCredentialsJwtUtils {

    public static final String VERIFIABLE_CREDENTIAL_CLAIM_KEY = "vc";

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, String subject, ECKey jwk) throws Exception {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder()
                .claim(VERIFIABLE_CREDENTIAL_CLAIM_KEY, credential)
                .issuer(issuer)
                .subject(subject)
                .build();

        var jws = new SignedJWT(jwsHeader, claims);

        jws.sign(new ECDSASigner(jwk.toECPrivateKey()));

        return SignedJWT.parse(jws.serialize());
    }

    public static ECKey readEcKey(File file) throws IOException, JOSEException {
        var contents = Files.readString(file.toPath());
        var jwk = ECKey.parseFromPEMEncodedObjects(contents);
        return jwk.toECKey();
    }
}
