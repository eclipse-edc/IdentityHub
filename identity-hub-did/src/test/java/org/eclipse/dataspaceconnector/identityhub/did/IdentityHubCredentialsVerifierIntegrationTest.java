package org.eclipse.dataspaceconnector.identityhub.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.identityhub.models.credentials.VerifiableCredential;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class IdentityHubCredentialsVerifierIntegrationTest {
    private static final int PORT = getFreePort();
    private static final String API_URL = String.format("http://localhost:%d/api/identity-hub", PORT);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private IdentityHubClient identityHubClient;

    @BeforeEach
    void setUp(EdcExtension extension) {
        var okHttpClient = TestUtils.testOkHttpClient();
        identityHubClient = new IdentityHubClientImpl(okHttpClient, OBJECT_MAPPER);
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(PORT), "edc.identity.hub.url", API_URL));
    }

    @Test
    public void getVerifiedClaims_getValidClaims() throws Exception {
        var publicKeyResolver = mock(DidPublicKeyResolverImpl.class);
        var credentialIssuer = "issuer1";
        var jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();
        var publicKey = new EllipticCurvePublicKey(jwk.getCurve().getName(), jwk.getKeyType().getValue(), jwk.getX().toString(), jwk.getY().toString());
        var keyWrapper = KeyConverter.toPublicKeyWrapper(publicKey, "ec");

        when(publicKeyResolver.resolvePublicKey(credentialIssuer)).thenReturn(Result.success(keyWrapper));

        var identityHubCredentialVerifier = new IdentityHubCredentialsVerifier(identityHubClient, new ConsoleMonitor(), publicKeyResolver);
        var didDocument = DidDocument.Builder.newInstance().service(List.of(new Service("IdentityHub", "IdentityHub", API_URL))).build();
        var credential = VerifiableCredential.Builder.newInstance()
                .id("id")
                .credentialSubject(Map.of("region", "eu"))
                .build();
        var jwt = buildSignedJwt(credential,  credentialIssuer, jwk);

        identityHubClient.addVerifiableCredential(API_URL, jwt);
        var claims = identityHubCredentialVerifier.verifyCredentials(didDocument);
        assertThat(claims.succeeded());
        assertThat(claims.getContent().size()).isEqualTo(1);
    }
}
