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

package org.eclipse.edc.identityhub.publickey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KeyPairResourcePublicKeyResolverTest {

    private final LocalPublicKeyService fallbackService = mock();
    private final KeyPairResourceStore resourceStore = mock();
    private final KeyParserRegistry parserRegistry = new KeyParserRegistryImpl();
    private final Monitor monitor = mock();
    private final KeyPairResourcePublicKeyResolver resolver = new KeyPairResourcePublicKeyResolver(resourceStore, parserRegistry, monitor, fallbackService);

    @BeforeEach
    void setUp() {
        parserRegistry.register(new JwkParser(new ObjectMapper(), monitor));
    }

    @Test
    void resolveKey_whenFound() {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(Collections.singletonList(createKeyPairResource().build())));

        assertThat(resolver.resolveKey("test-key", "participantId")).isSucceeded();
        verify(resourceStore).query(any(QuerySpec.class));
        verifyNoMoreInteractions(resourceStore);
        verifyNoInteractions(fallbackService, monitor);
    }

    @Test
    void resolveKey_whenNotFoundInStore_foundInVault() throws JOSEException {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(Collections.emptyList()));
        when(fallbackService.resolveKey(anyString())).thenReturn(Result.success(createPublicKeyJwk().toPublicKey()));

        assertThat(resolver.resolveKey("test-key", "participantId")).isSucceeded();

        verify(resourceStore).query(any(QuerySpec.class));
        verify(fallbackService).resolveKey(anyString());
        verify(monitor).warning(contains("Will attempt to resolve from the Vault."));
        verifyNoMoreInteractions(fallbackService, resourceStore);
    }

    @Test
    void resolveKey_whenStoreFailure() {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.notFound("foo-bar"));

        assertThat(resolver.resolveKey("test-key", "participantId")).isFailed().detail().isEqualTo("foo-bar");

        verify(resourceStore).query(any(QuerySpec.class));
        verify(monitor).warning(contains("Error querying database for KeyPairResource"));
        verifyNoMoreInteractions(fallbackService, resourceStore);
    }

    @Test
    void resolveKey_whenNotFoundInResourceStore_notFoundInVault() {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(Collections.emptyList()));
        when(fallbackService.resolveKey(anyString())).thenReturn(Result.failure("not found"));

        assertThat(resolver.resolveKey("test-key", "participantId")).isFailed()
                .detail()
                .contains("not found");

        verify(resourceStore).query(any(QuerySpec.class));
        verify(fallbackService).resolveKey(anyString());
        verify(monitor).warning(contains("Will attempt to resolve from the Vault."));
        verifyNoMoreInteractions(fallbackService, resourceStore);
    }

    @Test
    void resolveKey_whenMultipleFoundInStore() {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(List.of(
                createKeyPairResource().build(),
                createKeyPairResource().build()
        )));

        assertThat(resolver.resolveKey("test-key", "participantId")).isSucceeded();
        verify(resourceStore).query(any(QuerySpec.class));
        verify(monitor).warning(matches("Expected exactly 1 KeyPairResource with keyId '.*' but found '2'."));
        verifyNoMoreInteractions(resourceStore, monitor);
    }

    @Test
    void resolveKey_whenNotPublicKey() throws JOSEException {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(Collections.singletonList(createKeyPairResource()
                .serializedPublicKey(new OctetKeyPairGenerator(Curve.Ed25519).generate().toJSONString()).build())));

        assertThat(resolver.resolveKey("test-key", "participantId")).isFailed()
                .detail().contains("The specified resource did not contain public key material.");
        verify(resourceStore).query(any(QuerySpec.class));
        verifyNoMoreInteractions(resourceStore);
        verifyNoInteractions(fallbackService, monitor);
    }

    @Test
    void resolveKey_whenInvalidFormat() {
        when(resourceStore.query(any(QuerySpec.class))).thenReturn(StoreResult.success(Collections.singletonList(createKeyPairResource()
                .serializedPublicKey("this-is-not-jwk-or-pem").build())));

        assertThat(resolver.resolveKey("test-key", "participantId")).isFailed()
                .detail().contains("No parser found that can handle that format.");
        verify(resourceStore).query(any(QuerySpec.class));
        verifyNoMoreInteractions(resourceStore);
        verifyNoInteractions(fallbackService, monitor);
    }

    private KeyPairResource.Builder createKeyPairResource() {
        return KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .keyId(UUID.randomUUID().toString())
                .isDefaultPair(true)
                .state(KeyPairState.ACTIVATED)
                .serializedPublicKey(createPublicKeyJwk().toJSONString())
                .privateKeyAlias("test-key-alias");
    }

    private ECKey createPublicKeyJwk() {
        try {
            return new ECKeyGenerator(Curve.P_521).generate().toPublicJWK();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}