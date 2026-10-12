/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.DEFAULT_RENEWAL_PERIOD_SECONDS;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.DEFAULT_RENEWAL_THRESHOLD_SECONDS;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.DEFAULT_VALIDITY_SECONDS;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.MIN_BITSTRING_SIZE;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class BitstringStatusListConfigurationTest {

    private final Monitor monitor = mock();

    @Test
    void validated_shouldKeepValidValues() {
        var configuration = configuration(MIN_BITSTRING_SIZE * 2, 3600).validated(monitor);

        assertThat(configuration.bitstringSize()).isEqualTo(MIN_BITSTRING_SIZE * 2);
        assertThat(configuration.validity()).isEqualTo(Duration.ofHours(1));
        assertThat(configuration.renewalPeriodSeconds()).isEqualTo(DEFAULT_RENEWAL_PERIOD_SECONDS);
        assertThat(configuration.renewalThreshold()).isEqualTo(Duration.ofSeconds(DEFAULT_RENEWAL_THRESHOLD_SECONDS));
        verifyNoInteractions(monitor);
    }

    @Test
    void validated_whenLengthBelowMinimum_shouldUseMinimumAndWarn() {
        var configuration = configuration(16 * 1024, 3600).validated(monitor);

        assertThat(configuration.bitstringSize()).isEqualTo(MIN_BITSTRING_SIZE);
        verify(monitor).warning(contains("below the minimum"));
    }

    @Test
    void validated_whenLengthNotMultipleOf8_shouldRoundUpAndWarn() {
        var configuration = configuration(MIN_BITSTRING_SIZE + 1, 3600).validated(monitor);

        assertThat(configuration.bitstringSize()).isEqualTo(MIN_BITSTRING_SIZE + 8);
        verify(monitor).warning(contains("not a multiple of 8"));
    }

    @Test
    void validated_whenValidityNotPositive_shouldUseDefaultAndWarn() {
        var configuration = configuration(MIN_BITSTRING_SIZE, 0).validated(monitor);

        assertThat(configuration.validity()).isEqualTo(Duration.ofSeconds(DEFAULT_VALIDITY_SECONDS));
        verify(monitor).warning(contains("not positive"));
    }

    private BitstringStatusListConfiguration configuration(int bitstringSize, long validitySeconds) {
        return new BitstringStatusListConfiguration(bitstringSize, validitySeconds, DEFAULT_RENEWAL_PERIOD_SECONDS, DEFAULT_RENEWAL_THRESHOLD_SECONDS);
    }
}
