package org.eclipse.dataspaceconnector.identityhub.credentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;

public class JwtPayloadParserTest {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final JwtPayloadParser PAYLOAD_PARSER = new JwtPayloadParser(OBJECT_MAPPER);
    static final Faker FAKER = new Faker();
    static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    static final JWSHeader JWS_HEADER = new JWSHeader.Builder(JWSAlgorithm.ES256).build();

    @Test
    public void extractCredential_jwtWithValidCredential() throws Exception {

        // Arrange
        var verifiableCredential = generateVerifiableCredential();
        var issuer = FAKER.lorem().word();
        var subject = FAKER.lorem().word();
        var jwt = buildSignedJwt(verifiableCredential, issuer, subject);

        // Act
        var result = PAYLOAD_PARSER.extractCredential(jwt);

        // Assert
        assertThat(result.succeeded());
        assertThat(result.getContent())
                .usingRecursiveComparison()
                .ignoringFields(String.format("value.exp", verifiableCredential.getId()))
                .isEqualTo(toMap(verifiableCredential, issuer, subject).entrySet().stream().findFirst().get());
    }

    @Test
    public void extractCredential_jwtWithMissingVcField() {

        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(FAKER.lorem().word(), FAKER.lorem().word()).build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = PAYLOAD_PARSER.extractCredential(jws);

        // Assert
        assertThat(result.failed());
        assertThat(result.getFailureMessages()).containsExactly(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
    }

    @Test
    public void extractCredential_jwtWithWrongFormat() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(VERIFIABLE_CREDENTIALS_KEY, FAKER.lorem().word()).build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = PAYLOAD_PARSER.extractCredential(jws);

        // Assert
        assertThat(result.failed());
    }
}
