/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did;

import org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.did.DidConstants.DID_WEB_METHOD;
import static org.mockito.Mockito.mock;

class DidDocumentPublisherRegistryImplTest {
    private final DidDocumentPublisherRegistryImpl registry = new DidDocumentPublisherRegistryImpl();

    @Test
    void getPublisher_exists() {
        var mockedPublisher = mock(DidDocumentPublisher.class);
        registry.addPublisher(DID_WEB_METHOD, mockedPublisher);
        assertThat(registry.getPublisher(DID_WEB_METHOD + ":foobar")).isEqualTo(mockedPublisher);
    }

    @Test
    void getPublisher_whenOnlyMethod() {
        var mockedPublisher = mock(DidDocumentPublisher.class);
        registry.addPublisher(DID_WEB_METHOD, mockedPublisher);
        assertThat(registry.getPublisher(DID_WEB_METHOD)).isEqualTo(mockedPublisher);
        assertThat(registry.getPublisher(DID_WEB_METHOD + ":")).isEqualTo(mockedPublisher);
    }

    @Test
    void getPublisher_notExists() {
        assertThat(registry.getPublisher(DID_WEB_METHOD + ":foobar")).isNull();
    }

    @Test
    void getPublisher_invalidDid() {
        assertThatThrownBy(() -> registry.getPublisher("web:foobar")).isInstanceOf(IllegalArgumentException.class);
    }
}