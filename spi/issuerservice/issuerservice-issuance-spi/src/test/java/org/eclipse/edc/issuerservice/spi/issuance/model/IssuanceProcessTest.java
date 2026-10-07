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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuanceProcessTest {

    @DisplayName("B3.6: APPROVED -> DELIVERING is a legal transition")
    @Test
    void transitionToDelivering_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        process.transitionToDelivering();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERING.code());
    }

    @DisplayName("B3.6: DELIVERING -> DELIVERING (retry) is legal and increments the state count")
    @Test
    void transitionToDelivering_fromDelivering_succeeds() {
        var process = createProcess(IssuanceProcessStates.DELIVERING);
        var stateCountBefore = process.getStateCount();

        process.transitionToDelivering();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERING.code());
        assertThat(process.getStateCount()).isEqualTo(stateCountBefore + 1);
    }

    @DisplayName("B3.6: DELIVERING -> DELIVERED is a legal transition")
    @Test
    void transitionToDelivered_fromDelivering_succeeds() {
        var process = createProcess(IssuanceProcessStates.DELIVERING);

        process.transitionToDelivered();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
    }

    @DisplayName("B3.6: DELIVERING -> ERRORED is a legal transition")
    @Test
    void transitionToError_fromDelivering_succeeds() {
        var process = createProcess(IssuanceProcessStates.DELIVERING);

        process.transitionToError();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.ERRORED.code());
    }

    // credentials are only delivered once they are recorded, which happens on the way to DELIVERING
    @DisplayName("B3.6: APPROVED -> DELIVERED throws IllegalStateException")
    @Test
    void transitionToDelivered_fromApproved_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        assertThatThrownBy(process::transitionToDelivered).isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("B3.6: DELIVERING -> APPROVED throws IllegalStateException")
    @Test
    void transitionToApproved_fromDelivering_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.DELIVERING);

        assertThatThrownBy(process::transitionToApproved).isInstanceOf(IllegalStateException.class);
    }

    // B3.6: legal transition APPROVED -> APPROVED (retry) succeeds
    @DisplayName("B3.6: APPROVED -> APPROVED (retry) is legal and increments the state count")
    @Test
    void transitionToApproved_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);
        var stateCountBefore = process.getStateCount();

        process.transitionToApproved();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.APPROVED.code());
        assertThat(process.getStateCount()).isEqualTo(stateCountBefore + 1);
    }

    // B3.6: legal transition APPROVED -> ERRORED succeeds
    @DisplayName("B3.6: APPROVED -> ERRORED is a legal transition")
    @Test
    void transitionToError_fromApproved_succeeds() {
        var process = createProcess(IssuanceProcessStates.APPROVED);

        process.transitionToError();

        assertThat(process.getState()).isEqualTo(IssuanceProcessStates.ERRORED.code());
    }

    // B3.6: illegal transition DELIVERED -> APPROVED throws IllegalStateException
    @DisplayName("B3.6: DELIVERED -> APPROVED throws IllegalStateException")
    @Test
    void transitionToApproved_fromDelivered_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.DELIVERED);

        assertThatThrownBy(process::transitionToApproved).isInstanceOf(IllegalStateException.class);
    }

    // B3.6: illegal transition ERRORED -> DELIVERED throws IllegalStateException
    @DisplayName("B3.6: ERRORED -> DELIVERED throws IllegalStateException")
    @Test
    void transitionToDelivered_fromErrored_throwsIllegalStateException() {
        var process = createProcess(IssuanceProcessStates.ERRORED);

        assertThatThrownBy(process::transitionToDelivered).isInstanceOf(IllegalStateException.class);
    }

    // B3.6: illegal transition DELIVERED -> ERRORED throws IllegalStateException
    @DisplayName("B3.6: DELIVERED -> ERRORED throws IllegalStateException")
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
