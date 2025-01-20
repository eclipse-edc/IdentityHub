/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.sts.accountservice;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@ComponentTest
public class RemoteAccountServiceIntegrationTest {

    public static final String STS_ACCOUNTS_API_KEY = "password";
    private static final int ACCOUNTS_PORT = getFreePort();
    @RegisterExtension
    protected static final RuntimeExtension STS_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "STS",
            Map.of(
                    "web.http.port", "" + getFreePort(),
                    "web.http.path", "/",
                    "web.http.accounts.port", "" + ACCOUNTS_PORT,
                    "web.http.accounts.path", "/sts",
                    "web.http.version.port", "" + getFreePort()

            ),
            ":e2e-tests:runtimes:sts"
    ));
    private static final int IDENTITY_PORT = getFreePort();
    @RegisterExtension
    protected static final RuntimeExtension IDENTITYHUB_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "IdentityHub",
            new HashMap<>() {
                {
                    put(PARTICIPANT_ID, UUID.randomUUID().toString());
                    put("web.http.port", String.valueOf(getFreePort()));
                    put("web.http.path", "/api/v1");
                    put("web.http.presentation.port", "" + getFreePort());
                    put("web.http.presentation.path", "/presentation");
                    put("web.http.identity.port", "" + IDENTITY_PORT);
                    put("web.http.identity.path", "/identity");
                    put("web.http.did.port", "" + getFreePort()); // avoid conflicts with other tests if they run in parallel
                    put("web.http.did.path", "/");
                    put("web.http.version.port", "" + getFreePort()); // avoid conflicts with other tests if they run in parallel
                    put("edc.runtime.id", "identityhub");
                    put("edc.ih.iam.id", "did:web:consumer");
                    put("edc.sts.account.api.url", "http://localhost:" + ACCOUNTS_PORT + "/sts");
                    put("edc.sts.accounts.api.auth.header.value", STS_ACCOUNTS_API_KEY);
                }
            },
            ":e2e-tests:runtimes:identityhub-remote-sts"
    ));

    @BeforeEach
    void setup() {
    }

    @Test
    void createParticipant_expectStsAccount(StsAccountService accountService, StsAccountStore accountStore) {

        var vault = STS_RUNTIME.getService(Vault.class);
        var manifest = createNewParticipant().build();

        assertThat(accountService.createAccount(manifest, "test-alias")).isSucceeded();

        assertThat(accountStore.findById(manifest.getDid())).isNotNull();
        assertThat(vault.resolveSecret("test-alias")).isNotNull();
    }

    @Test
    void updateAccount(StsAccountService accountService, StsAccountStore accountStore) {

        var originalAccountBuilder = createStsAccount();
        var originalAccount = originalAccountBuilder.build();
        accountStore.create(originalAccount);

        // update account
        var updatedAccount = originalAccountBuilder
                .did("did:web:new-did")
                .build();

        assertThat(accountService.updateAccount(updatedAccount)).isSucceeded();
        assertThat(accountStore.findById(updatedAccount.getId())).isSucceeded()
                .extracting(StsAccount::getDid)
                .isEqualTo(updatedAccount.getDid());
    }


    @Test
    void updateAccount_whenNotExists_expect404(StsAccountService accountService) {
        var account = createStsAccount().build();

        assertThat(accountService.updateAccount(account)).isFailed()
                .detail().isEqualTo("Not Found");
    }

    @Test
    void deleteAccount(StsAccountService accountService, StsAccountStore accountStore) {

        var account = createStsAccount().build();
        accountStore.create(account);


        assertThat(accountService.deleteAccount(account.getId())).isSucceeded();
        assertThat(accountStore.findById(account.getId())).isFailed().detail().contains("not found");
    }


    @Test
    void deleteAccount_whenNotExists_expect404(StsAccountService accountService) {
        var account = createStsAccount().build();

        assertThat(accountService.deleteAccount(account.getId())).isFailed()
                .detail().isEqualTo("Not Found");
    }

    @Test
    void findById(StsAccountService accountService, StsAccountStore accountStore) {
        var account = createStsAccount().build();
        accountStore.create(account);


        assertThat(accountService.findById(account.getId())).isSucceeded();
        assertThat(accountStore.findById(account.getId())).isSucceeded();
    }

    @Test
    void findById_whenNotExists_expect404(StsAccountService accountService) {
        var account = createStsAccount().build();

        assertThat(accountService.findById(account.getId())).isFailed()
                .detail().isEqualTo("Not Found");
    }

    public ParticipantManifest.Builder createNewParticipant() {
        var id = UUID.randomUUID().toString();
        return ParticipantManifest.Builder.newInstance()
                .participantId("test-participant-" + id)
                .active(false)
                .did("did:web:test:participant:" + id)
                .key(createKeyDescriptor().build());
    }

    public KeyDescriptor.Builder createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .privateKeyAlias("another-alias")
                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                .keyId("another-keyid");
    }

    private StsAccount.Builder createStsAccount() {
        var id = UUID.randomUUID().toString();
        return StsAccount.Builder.newInstance()
                .id(id)
                .name(id)
                .clientId("did:web:participant:" + id)
                .did("did:web:participant:" + id)
                .privateKeyAlias("private-key-alias-" + id)
                .publicKeyReference("did:web:participant:%s#key1".formatted(id))
                .secretAlias("secret-alias-" + id);
    }
}
