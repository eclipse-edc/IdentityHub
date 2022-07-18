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

package org.eclipse.dataspaceconnector.identityhub.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.identityhub.model.credentials.VerifiableCredential;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityHubCredentialsVerifierTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void getVerifiedClaims_getValidClaims() throws Exception {
        var credId = FAKER.internet().uuid();
        var hubBaseUrl = "https://" + FAKER.internet().url();
        var issuer = FAKER.internet().url();
        var jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();

        var identityHubClient = mock(IdentityHubClient.class);
        var monitor = new ConsoleMonitor();
        var didPublicKeyResolver = mock(DidPublicKeyResolver.class);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, monitor, didPublicKeyResolver);
        var didDocument = DidDocument.Builder.newInstance().service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        var credential = VerifiableCredential.Builder.newInstance().id(credId).build();

        var jws = buildSignedJwt(credential, issuer, jwk);
        var clientResult = StatusResult.success((Collection<SignedJWT>) List.of(jws));
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl)).thenReturn(clientResult);
        var publicKey = new EllipticCurvePublicKey(jwk.getCurve().getName(), jwk.getKeyType().getValue(), jwk.getX().toString(), jwk.getY().toString());
        when(didPublicKeyResolver.resolvePublicKey(issuer)).thenReturn(Result.success(KeyConverter.toPublicKeyWrapper(publicKey, "ec")));

        var credentials = credentialsVerifier.verifyCredentials(didDocument);
    }

    @Test
    public void getVerifiedClaims_filtersSignedByWrongIssuer() {

    }

    @Test
    public void getVerifiedClaims_filtersClaimsWithWrongFormat() {

    }

    // TODO: Move it to some common TestUtil as it is duplicated in IdentityHubClientTest.
    private IdentityHubClientImpl createClient(Interceptor interceptor) {
        var okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        return new IdentityHubClientImpl(okHttpClient, OBJECT_MAPPER);
    }

}
