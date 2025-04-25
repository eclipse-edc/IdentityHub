/*
 *  Copyright (c) 2025 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.publisher;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class LocalStatusListCredentialPublisherExtensionTest {

    private static final String CUSTOM_STATUS_LIST_CALLBACK_ADDRESS = "edc.statuslist.callback.address";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9999;
    private static final String DEFAULT_STATUS_LIST_PATH = "/statuslist";
    private static final String CUSTOM_CALLBACK_ADDRESS = "http://example.com" + DEFAULT_STATUS_LIST_PATH;
    private static final String RAW_CREDENTIAL = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private static final String CREDENTIAL_ID = "credentialId";
    private static final String TEST_CREDENTIAL = "TestCredential";
    private static final String DID_WEB_ISSUER = "did:web:issuer";
    private final VerifiableCredential credential = VerifiableCredential.Builder.newInstance()
            .type(TEST_CREDENTIAL)
            .credentialSubject(new CredentialSubject())
            .issuer(new Issuer(DID_WEB_ISSUER))
            .issuanceDate(Instant.now())
            .id(CREDENTIAL_ID)
            .build();

    private final VerifiableCredentialResource credentialResource = VerifiableCredentialResource.Builder.newInstance()
            .issuerId(DID_WEB_ISSUER)
            .holderId(DID_WEB_ISSUER)
            .credential(new VerifiableCredentialContainer(RAW_CREDENTIAL, CredentialFormat.VC1_0_JWT, credential))
            .build();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        TransactionContext transactionContext = spy(TransactionContext.class);
        CredentialStore credentialStore = mock(CredentialStore.class);
        Hostname hostname = mock(Hostname.class);
        context.registerService(TransactionContext.class, transactionContext);
        context.registerService(CredentialStore.class, credentialStore);
        context.registerService(Hostname.class, hostname);

        when(transactionContext.execute((TransactionContext.ResultTransactionBlock<String>) any())).thenAnswer(invocation -> {
            TransactionContext.ResultTransactionBlock<String> block = invocation.getArgument(0);
            return block.execute();
        });
        StoreResult<VerifiableCredentialResource> verifiableCredentialResourceStoreResult = StoreResult.success(credentialResource);
        when(credentialStore.findById(any())).thenReturn(verifiableCredentialResourceStoreResult);
        when(credentialStore.update(credentialResource)).thenReturn(StoreResult.success());
        when(hostname.get()).thenReturn(DEFAULT_HOST);
    }

    @DisplayName("Verifies if custom callback address is set for status list")
    @Test
    void createInMemoryStatusListCredentialPublisher_whenCustomStatusListCallbackAddressIsSet_shouldSetStatusListWithCustomAddress(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(CUSTOM_STATUS_LIST_CALLBACK_ADDRESS, CUSTOM_CALLBACK_ADDRESS)));
        Result<String> result = getStatusListPublisherResult(factory);
        assertEquals(CUSTOM_CALLBACK_ADDRESS + "/" + CREDENTIAL_ID, result.getContent());
    }

    @DisplayName("Verifies if default callback address is set for status list")
    @Test
    void createInMemoryStatusListCredentialPublisher_whenCustomStatusListCallbackAddressIsNotSet_shouldSetStatusListWithDefaultAddress(ObjectFactory factory) {
        Result<String> result = getStatusListPublisherResult(factory);
        String defaultUrl = "http://%s:%s%s".formatted(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_STATUS_LIST_PATH);
        assertEquals(defaultUrl + "/" + CREDENTIAL_ID, result.getContent());
    }

    private static Result<String> getStatusListPublisherResult(ObjectFactory factory) {
        var localStatusListCredentialPublisherExtension = factory.constructInstance(LocalStatusListCredentialPublisherExtension.class);
        var statusListCredentialPublisher = localStatusListCredentialPublisherExtension.createInMemoryStatusListCredentialPublisher();
        assertThat(statusListCredentialPublisher).isInstanceOf(LocalCredentialPublisher.class);
        var credential = VerifiableCredential.Builder.newInstance().id("credentialId").type("any")
                .issuer(new Issuer("any")).credentialSubject(new CredentialSubject()).issuanceDate(Instant.now())
                .build();
        var resource = VerifiableCredentialResource.Builder.newInstance()
                .issuerId("any").holderId("any")
                .credential(new VerifiableCredentialContainer("any", CredentialFormat.VC1_0_JWT, credential))
                .build();
        return statusListCredentialPublisher.publish(resource);
    }
}
