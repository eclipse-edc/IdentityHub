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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.spi.observe.Observable;

import java.util.Collection;
import java.util.Map;

/**
 * Interface implemented by listeners registered to observe credential issuance changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface IssuanceEventListener {

    /**
     * A credential issuance request was received. A decision whether it is rejected or will be processed further is not
     * made yet.
     * An {@link IssuanceReceived} has been created to represent the request internally.
     */
    default void received(String holderPid, String participantContextId, Map<String, CredentialFormat> content) {

    }

    /**
     * A credential issuance request was successfully received and will be processed further by the Issuer service.
     * An {@link IssuanceRequested} has been created to represent the request internally.
     */
    default void requested(IssuanceProcess ip) {

    }

    /**
     * A credential issuance request was received but rejected by the Issuer service. No {@link IssuanceRejected} has been created.
     */
    default void rejected(String holderPid, String issuerParticipantContextId, String failureDetail) {

    }

    /**
     * The Issuer service approved a credential issuance request.
     */
    default void approved(IssuanceProcess process) {

    }

    /**
     * The credentials requested in the issuance request have been generated (signed).
     */
    default void generated(IssuanceProcess process, Collection<VerifiableCredentialContainer> creds) {

    }

    /**
     * The credentials requested in the issuance request have been delivered successfully to the holder.
     */
    default void delivered(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {

    }

    /**
     * A credential issuance request failed. The associated {@link IssuanceProcess} is in the {@link org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates#ERRORED}
     * state.
     */
    default void errored(IssuanceProcess process, Throwable throwable) {

    }
}
