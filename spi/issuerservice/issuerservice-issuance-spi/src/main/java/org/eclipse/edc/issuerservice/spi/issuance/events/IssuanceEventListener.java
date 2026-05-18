/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.spi.issuance.events;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.spi.observe.Observable;

import java.util.Collection;

/**
 * Interface implemented by listeners registered to observe credential issuance changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface IssuanceEventListener {

    default void received(IssuanceProcess ip) {

    }

    default void rejected(String holderPid, String issuerParticipantContextId, String failureDetail) {

    }

    default void approved(IssuanceProcess process) {

    }

    default void generated(IssuanceProcess process, Collection<VerifiableCredentialContainer> creds) {

    }

    default void delivered(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {

    }

    default void errored(IssuanceProcess process, Throwable throwable) {

    }
}
