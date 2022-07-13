package org.eclipse.dataspaceconnector.identityhub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.models.credentials.VerifiableCredential;

public class VerifiableCredentialTestUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer) throws Exception {

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", OBJECT_MAPPER.writeValueAsString(credential))
                .issuer(issuer)
                .build();

        var jws = new SignedJWT(jwsHeader, claims);

        // sign jwt
        var jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();
        var jwsSigner = new ECDSASigner(jwk.toECPrivateKey());
        jws.sign(jwsSigner);
        return jws;
    }
}
