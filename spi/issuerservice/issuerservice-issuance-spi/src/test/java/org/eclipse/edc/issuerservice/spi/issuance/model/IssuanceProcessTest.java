/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.issuance.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuanceProcessTest {

    // B3.6: legal transition APPROVED -> DELIVERED succeeds
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToDelivered_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        process.transitionToDelivered();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
    }

    // B3.6: legal transition APPROVED -> APPROVED (retry) succeeds
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToApproved_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        process.transitionToApproved();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.APPROVED.code());
        // TODO: assert the stateCount was incremented (retry bookkeeping)
    }

    // B3.6: legal transition APPROVED -> ERRORED succeeds
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToError_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        process.transitionToError();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.ERRORED.code());
    }

    // B3.6: illegal transition DELIVERED -> APPROVED throws IllegalStateException
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToApproved_fromDelivered_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.DELIVERED);

        assertThatThrownBy(process::transitionToApproved).isInstanceOf(IllegalStateException.class);
    }

    // B3.6: illegal transition ERRORED -> DELIVERED throws IllegalStateException
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToDelivered_fromErrored_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.ERRORED);

        assertThatThrownBy(process::transitionToDelivered).isInstanceOf(IllegalStateException.class);
    }

    // B3.6: illegal transition DELIVERED -> ERRORED throws IllegalStateException
    @Disabled("TODO: implement (catalog B3.6)")
    @Test
    void transitionToError_fromDelivered_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.DELIVERED);

        assertThatThrownBy(process::transitionToError).isInstanceOf(IllegalStateException.class);
    }

    private IssuanceProcess createProcess(IssuanceProcessStates state) {
        return IssuanceProcess.Builder.newInstance()
                .id("test-process-id")
                .state(state.code())
                .holderId("holderId")
                .holderPid("holderPid")
                .participantContextId("participantContextId")
                .build();
    }
}
