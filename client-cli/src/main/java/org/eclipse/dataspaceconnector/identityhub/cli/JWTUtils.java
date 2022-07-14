package org.eclipse.dataspaceconnector.identityhub.cli;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.model.credentials.VerifiableCredential;

import java.io.File;
import java.nio.file.Files;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.util.Date;

import static java.time.Instant.now;

class JWTUtils {

    public static SignedJWT buildSignedJwt(VerifiableCredential credential, String issuer, ECPrivateKey privateKey) throws Exception {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder()
                .claim("vc", credential)
                .issuer(issuer)
                .audience("identity-hub")
                .expirationTime(Date.from(now().plus(Duration.ofDays(15))))
                .subject("verifiable-credential")
                .build();

        var jws = new SignedJWT(jwsHeader, claims);

        var jwsSigner = new ECDSASigner(privateKey);
        jws.sign(jwsSigner);

        return SignedJWT.parse(jws.serialize());
    }

    public static ECPrivateKey readECPrivateKey(File file) throws Exception {
        String contents = Files.readString(file.toPath());
        JWK jwk = ECKey.parseFromPEMEncodedObjects(contents);
        return jwk.toECKey().toECPrivateKey();
    }
}
