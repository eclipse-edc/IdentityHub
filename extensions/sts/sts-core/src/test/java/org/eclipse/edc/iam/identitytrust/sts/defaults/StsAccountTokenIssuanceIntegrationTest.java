/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.iam.identitytrust.sts.defaults.store.InMemoryStsAccountStore;
import org.eclipse.edc.iam.identitytrust.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccountTokenAdditionalParams;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.sts.accountservice.RandomStringGenerator;
import org.eclipse.edc.identityhub.sts.accountservice.StsAccountServiceImpl;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.VaultPrivateKeyResolver;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClientBuilder;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
public class StsAccountTokenIssuanceIntegrationTest {

    private final InMemoryStsAccountStore clientStore = new InMemoryStsAccountStore(CriterionOperatorRegistryImpl.ofDefaults());
    private final Vault vault = new InMemoryVault(mock());
    private final KeyParserRegistry keyParserRegistry = new KeyParserRegistryImpl();
    private final JtiValidationStore jtiValidationStore = mock();
    private StsAccountServiceImpl clientService;
    private StsClientTokenGeneratorServiceImpl tokenGeneratorService;

    @BeforeEach
    void setup() {
        clientService = new StsAccountServiceImpl(clientStore, new NoopTransactionContext(), vault, new RandomStringGenerator());

        keyParserRegistry.register(new PemParser(mock()));
        keyParserRegistry.register(new JwkParser(new ObjectMapper(), mock()));
        var privateKeyResolver = new VaultPrivateKeyResolver(keyParserRegistry, vault, mock(), mock());

        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());

        tokenGeneratorService = new StsClientTokenGeneratorServiceImpl(60 * 5, new EmbeddedSecureTokenService(new NoopTransactionContext(), 60 * 5,
                new JwtGenerationService(new DefaultJwsSignerProvider(privateKeyResolver)), Clock.systemUTC(), clientService));
    }

    @Test
    void authenticateAndGenerateToken() throws Exception {
        var participantId = "participant_id";
        var clientId = "client_id";
        var secretAlias = "client_secret_alias";
        var privateKeyAlias = "client_id";
        var audience = "aud";
        var did = "did:example:subject";
        var client = createClientBuilder(participantId)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlias)
                .secretAlias(secretAlias)
                .publicKeyReference("public-key")
                .did(did)
                .build();

        var additional = StsAccountTokenAdditionalParams.Builder.newInstance().audience(audience).build();

        vault.storeSecret(privateKeyAlias, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.createAccount(ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .keyId("public-key")
                        .privateKeyAlias(privateKeyAlias)
                        .build())
                .build(), secretAlias);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);

        AbstractResultAssert.assertThat(tokenResult).isSucceeded();

        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                .doesNotContainKey(CLIENT_ID);

    }

    @Test
    void authenticateAndGenerateToken_withBearerAccessScope() throws Exception {
        var participantId = "participant_id";
        var clientId = "client_id";
        var secretAlias = "client_secret_alias";
        var privateKeyAlias = "client_id";
        var did = "did:example:subject";
        var audience = "aud";
        var scope = "scope:test";
        var client = createClientBuilder(participantId)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlias)
                .secretAlias(secretAlias)
                .did(did)
                .publicKeyReference("public-key")
                .build();

        var additional = StsAccountTokenAdditionalParams.Builder.newInstance().audience(audience).bearerAccessScope(scope).build();

        vault.storeSecret(privateKeyAlias, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.createAccount(ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .keyId("public-key")
                        .privateKeyAlias(privateKeyAlias)
                        .build())
                .build(), secretAlias);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT, PRESENTATION_TOKEN_CLAIM)
                .doesNotContainKey(CLIENT_ID);

    }

    @Test
    void authenticateAndGenerateToken_withAccessToken() throws Exception {
        var participantId = "participant_id";
        var clientId = "client_id";
        var secretAlias = "client_secret_alias";
        var privateKeyAlias = "client_id";
        var audience = "aud";
        var accessToken = "tokenTest";
        var did = "did:example:subject";

        var client = createClientBuilder(participantId)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlias)
                .secretAlias(secretAlias)
                .publicKeyReference("public-key")
                .did(did)
                .build();

        var additional = StsAccountTokenAdditionalParams.Builder.newInstance().audience(audience).accessToken(accessToken).build();

        vault.storeSecret(privateKeyAlias, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.createAccount(ParticipantManifest.Builder.newInstance()
                .participantId(participantId)
                .did(did)
                .key(KeyDescriptor.Builder.newInstance()
                        .keyId("public-key")
                        .privateKeyAlias(privateKeyAlias)
                        .build())
                .build(), secretAlias);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(PRESENTATION_TOKEN_CLAIM, accessToken)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                .doesNotContainKey(CLIENT_ID);

    }

    /**
     * Load content from a resource file.
     */
    private String loadResourceFile(String file) throws IOException {
        try (var resourceAsStream = StsAccountTokenIssuanceIntegrationTest.class.getClassLoader().getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(resourceAsStream).readAllBytes());
        }
    }
}
