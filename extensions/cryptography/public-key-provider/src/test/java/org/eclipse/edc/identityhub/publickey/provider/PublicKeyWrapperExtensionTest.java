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

package org.eclipse.edc.identityhub.publickey.provider;

import com.nimbusds.jose.JOSEException;
import org.eclipse.edc.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.edc.iam.did.crypto.key.RsaPublicKeyWrapper;
import org.eclipse.edc.identityhub.publickey.resolver.PublicKeyWrapperExtension;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.publickey.resolver.PublicKeyWrapperExtension.PUBLIC_KEY_PATH_PROPERTY;
import static org.eclipse.edc.identityhub.publickey.resolver.PublicKeyWrapperExtension.PUBLIC_KEY_VAULT_ALIAS_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class PublicKeyWrapperExtensionTest {

    public static final String PEMFILE_NAME = "testkey.pem";
    private static final String JWKFILE_NAME = "testkey.json";
    private final Vault vaultMock = mock();
    private PublicKeyWrapperExtension extension;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(Vault.class, vaultMock);
        this.extension = factory.constructInstance(PublicKeyWrapperExtension.class);
    }

    @Test
    void createPublicKeyWrapper_fromVaultPem(ServiceExtensionContext context) {
        when(context.getSetting(eq(PUBLIC_KEY_VAULT_ALIAS_PROPERTY), any())).thenReturn("foo");
        when(vaultMock.resolveSecret(eq("foo"))).thenReturn(getPem());

        var wrapper = extension.createPublicKey(context);

        assertThat(wrapper).isInstanceOf(RsaPublicKeyWrapper.class);
    }


    @Test
    void createPublicKeyWrapper_fromVaultJwk(ServiceExtensionContext context) {
        when(context.getSetting(eq(PUBLIC_KEY_VAULT_ALIAS_PROPERTY), any())).thenReturn("foo");
        when(vaultMock.resolveSecret(eq("foo"))).thenReturn(getJwk());

        var wrapper = extension.createPublicKey(context);

        assertThat(wrapper).isInstanceOf(EcPublicKeyWrapper.class);
    }


    @Test
    void createPublicKeyWrapper_fromFilePem(ServiceExtensionContext context) {
        var file = TestUtils.getFileFromResourceName(PEMFILE_NAME);
        when(context.getSetting(eq(PUBLIC_KEY_PATH_PROPERTY), any())).thenReturn(file.getAbsolutePath());

        var wrapper = extension.createPublicKey(context);

        assertThat(wrapper).isInstanceOf(RsaPublicKeyWrapper.class);
        verifyNoInteractions(vaultMock);
    }

    @Test
    void createPublicKeyWrapper_fromFileJwk(ServiceExtensionContext context) {
        var file = TestUtils.getFileFromResourceName(JWKFILE_NAME);
        when(context.getSetting(eq(PUBLIC_KEY_PATH_PROPERTY), any())).thenReturn(file.getAbsolutePath());

        var wrapper = extension.createPublicKey(context);

        assertThat(wrapper).isInstanceOf(EcPublicKeyWrapper.class);
        verifyNoInteractions(vaultMock);
    }

    @Test
    void createPublicKeyWrapper_fromVaultInvalidFormat(ServiceExtensionContext context) {
        when(context.getSetting(eq(PUBLIC_KEY_VAULT_ALIAS_PROPERTY), any())).thenReturn("foo");
        when(vaultMock.resolveSecret(eq("foo"))).thenReturn("some invalid string");

        assertThatThrownBy(() -> extension.createPublicKey(context)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(JOSEException.class);

    }

    @Test
    void createPublicKeyWrapper_fromFileInvalidFormat(ServiceExtensionContext context) {

        when(context.getSetting(eq(PUBLIC_KEY_PATH_PROPERTY), any())).thenReturn(TestUtils.getFileFromResourceName("invalidkey.txt").getAbsolutePath());
        assertThatThrownBy(() -> extension.createPublicKey(context)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(JOSEException.class);
        verifyNoInteractions(vaultMock);

    }

    @Test
    void createPublicKeyWrapper_notConfigured(ServiceExtensionContext context) {
        assertThatThrownBy(() -> extension.createPublicKey(context)).isInstanceOf(EdcException.class).hasMessage("No public key was configured! Please either configure 'edc.ih.iam.publickey.path' or 'edc.ih.iam.publickey.alias'.");

    }

    private String getPem() {
        return TestUtils.getResourceFileContentAsString(PEMFILE_NAME);
    }

    private String getJwk() {
        return TestUtils.getResourceFileContentAsString(JWKFILE_NAME);
    }
}