/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferObservable;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CredentialOfferServiceImplTest {

    private final CredentialOfferStore store = mock();
    private final CredentialOfferObservable observable = mock();
    private final CredentialOfferServiceImpl service = new CredentialOfferServiceImpl(store, new NoopTransactionContext(), observable);

    @Test
    void create_shouldEmitEvent() {
        var offer = CredentialOffer.Builder.newInstance()
                .issuer("issuer")
                .participantContextId("participantContextId")
                .state(CredentialOfferStatus.RECEIVED.code())
                .build();
        service.create(offer);

        verify(store).save(offer);
        verify(observable).invokeForEach(any());
    }

    @Test
    void create_whenStoreFails_shouldNotEmitEvent() {
        doThrow(new EdcPersistenceException("foo")).when(store).save(any());

        var offer = CredentialOffer.Builder.newInstance()
                .issuer("issuer")
                .participantContextId("participantContextId")
                .state(CredentialOfferStatus.RECEIVED.code())
                .build();
        var actual = service.create(offer);
        assertThat(actual).isFailed();
        assertThat(actual.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);

        verify(store).save(offer);
        verifyNoInteractions(observable);
    }
}