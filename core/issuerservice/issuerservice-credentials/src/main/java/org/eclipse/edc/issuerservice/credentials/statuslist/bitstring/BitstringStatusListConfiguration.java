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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.Duration;

/**
 * Configures the status lists: their length, how long a status list credential is valid after it was signed, and how
 * status list credentials that are about to expire are renewed. Use {@link #validated(Monitor)} to replace values that
 * cannot be used as they are.
 */
@Settings
public record BitstringStatusListConfiguration(
        @Setting(
                description = "The number of entries of a status list. A lower value than the minimum of the W3C Bitstring Status List, 131072, is raised to it",
                defaultValue = MIN_BITSTRING_SIZE + "", key = "edc.issuer.statuslist.bitstring.size"
        )
        int bitstringSize,

        @Setting(
                description = "How long a status list credential is valid after it was signed, in seconds",
                defaultValue = DEFAULT_VALIDITY_SECONDS + "", key = "edc.issuer.statuslist.validity"
        )
        long validitySeconds,

        @Setting(
                description = "How often status list credentials that are about to expire are renewed, in seconds. 0 or less deactivates the renewal",
                defaultValue = DEFAULT_RENEWAL_PERIOD_SECONDS + "", key = "edc.issuer.statuslist.renewal.period"
        )
        long renewalPeriodSeconds,

        @Setting(
                description = "How long before it expires a status list credential is renewed, in seconds",
                defaultValue = DEFAULT_RENEWAL_THRESHOLD_SECONDS + "", key = "edc.issuer.statuslist.renewal.threshold"
        )
        long renewalThresholdSeconds
) {

    /**
     * The minimum length of a bitstring according to the W3C Bitstring Status List: 16 KB, i.e. 131,072 entries.
     */
    public static final int MIN_BITSTRING_SIZE = 16 * 1024 * 8;
    public static final long DEFAULT_VALIDITY_SECONDS = 365L * 24 * 60 * 60;
    public static final long DEFAULT_RENEWAL_PERIOD_SECONDS = 24 * 60 * 60;
    public static final long DEFAULT_RENEWAL_THRESHOLD_SECONDS = 30 * 24 * 60 * 60;

    /**
     * Returns this configuration, with the values that cannot be used as they are replaced, and logs a warning for each of
     * them: a length below the minimum is raised to it, a length that is not a multiple of 8 is rounded up, and a validity
     * that is not positive is replaced by the default. If the renewal is active, a renewal threshold that is not positive
     * is replaced by the default too.
     * <p>
     * It also warns about renewal settings that can be used, but have unwanted effects: a renewal period that is not shorter
     * than the renewal threshold lets status list credentials expire between two renewals, and a renewal threshold that is
     * not shorter than the validity renews them on every run.
     */
    public BitstringStatusListConfiguration validated(Monitor monitor) {
        var validBitstringSize = bitstringSize;
        if (validBitstringSize < MIN_BITSTRING_SIZE) {
            monitor.warning("The configured status list length of %d is below the minimum of the W3C Bitstring Status List, using %d instead"
                    .formatted(validBitstringSize, MIN_BITSTRING_SIZE));
            validBitstringSize = MIN_BITSTRING_SIZE;
        }
        if (validBitstringSize % 8 != 0) {
            var roundedUp = (validBitstringSize / 8 + 1) * 8;
            monitor.warning("The configured status list length of %d is not a multiple of 8, using %d instead".formatted(validBitstringSize, roundedUp));
            validBitstringSize = roundedUp;
        }
        var validValiditySeconds = validitySeconds;
        if (validValiditySeconds <= 0) {
            monitor.warning("The configured status list credential validity of %d seconds is not positive, using %d seconds instead"
                    .formatted(validValiditySeconds, DEFAULT_VALIDITY_SECONDS));
            validValiditySeconds = DEFAULT_VALIDITY_SECONDS;
        }
        var validRenewalThresholdSeconds = renewalThresholdSeconds;
        if (renewalPeriodSeconds > 0) {
            if (validRenewalThresholdSeconds <= 0) {
                monitor.warning("The configured status list credential renewal threshold of %d seconds is not positive, using %d seconds instead"
                        .formatted(validRenewalThresholdSeconds, DEFAULT_RENEWAL_THRESHOLD_SECONDS));
                validRenewalThresholdSeconds = DEFAULT_RENEWAL_THRESHOLD_SECONDS;
            }
            if (renewalPeriodSeconds >= validRenewalThresholdSeconds) {
                monitor.warning("Status list credentials are renewed every %d seconds, which is not shorter than the renewal threshold of %d seconds, so they can expire between two renewals"
                        .formatted(renewalPeriodSeconds, validRenewalThresholdSeconds));
            }
            if (validRenewalThresholdSeconds >= validValiditySeconds) {
                monitor.warning("Status list credentials are renewed %d seconds before they expire, which is not shorter than their validity of %d seconds, so they are renewed on every run"
                        .formatted(validRenewalThresholdSeconds, validValiditySeconds));
            }
        }
        return new BitstringStatusListConfiguration(validBitstringSize, validValiditySeconds, renewalPeriodSeconds, validRenewalThresholdSeconds);
    }

    /**
     * How long a status list credential is valid after it was signed.
     */
    public Duration validity() {
        return Duration.ofSeconds(validitySeconds);
    }

    /**
     * How long before it expires a status list credential is renewed.
     */
    public Duration renewalThreshold() {
        return Duration.ofSeconds(renewalThresholdSeconds);
    }
}
