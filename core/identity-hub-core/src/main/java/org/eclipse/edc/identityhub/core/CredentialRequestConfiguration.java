/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record CredentialRequestConfiguration(
        @Setting(
                description = "Interval in milliseconds at which the Issuer is asked about the status of credential requests that are still awaiting their credentials. " +
                        "Each run is delayed by up to a fifth of it at random, so that runtimes do not poll in lockstep. " +
                        "Configuring a number <= 0 disables polling, and with it the timeouts of requests awaiting their credentials.",
                defaultValue = DEFAULT_STATUS_POLL_INTERVAL + "", key = "edc.iam.credential.request.status.poll.interval"
        )
        long statusPollInterval,

        @Setting(
                description = "Time in milliseconds a credential request waits for its credentials before the Issuer is first asked about its status, " +
                        "as most Issuers deliver them within that time",
                defaultValue = DEFAULT_STATUS_POLL_MIN_AGE + "", key = "edc.iam.credential.request.status.poll.min.age"
        )
        long statusPollMinAge,

        @Setting(
                description = "Maximum time in milliseconds between two status queries for the same credential request. A request is asked about less often the " +
                        "longer it waits for its credentials, up to this interval.",
                defaultValue = DEFAULT_STATUS_POLL_MAX_INTERVAL + "", key = "edc.iam.credential.request.status.poll.max.interval"
        )
        long statusPollMaxInterval,

        /*
            this setting  has been added only to permit interaction with an external system that's not 100% compliant with DCP v1.0, can be removed after v1.0 release
         */
        @Deprecated(since = "1.0.0")
        @Setting(
                description = "The scope that will be used to issue an access token to be included in the Self-Issued ID token in the 'token' claim. By default, no token will be generated.",
                required = false, key = "edc.iam.credential.request.bearer.access.scope"
        )
        String bearerAccessScope,

        @Setting(
                description = "Time in seconds after which a credential request is considered failed if its credentials have not arrived, and its status cannot be queried, because the Issuer did not report its issuance process ID",
                defaultValue = DEFAULT_UNTRACKED_TIMEOUT + "", key = "edc.iam.credential.request.untracked.timeout"
        )
        long untrackedTimeout,

        @Setting(
                description = "Time in seconds after which a credential request is considered failed if its credentials have not arrived, although the Issuer keeps reporting it as pending",
                defaultValue = DEFAULT_TRACKED_TIMEOUT + "", key = "edc.iam.credential.request.tracked.timeout"
        )
        long trackedTimeout
) {

    public static final long DEFAULT_STATUS_POLL_INTERVAL = 30_000;
    public static final long DEFAULT_STATUS_POLL_MIN_AGE = 30_000;
    public static final long DEFAULT_STATUS_POLL_MAX_INTERVAL = 15 * 60 * 1000;
    public static final long DEFAULT_UNTRACKED_TIMEOUT = 6 * 60 * 60;
    public static final long DEFAULT_TRACKED_TIMEOUT = 24 * 60 * 60;

    @Override
    public long untrackedTimeout() {
        if (untrackedTimeout == 0) {
            return DEFAULT_UNTRACKED_TIMEOUT;
        }
        return untrackedTimeout;
    }

    @Override
    public long trackedTimeout() {
        if (trackedTimeout == 0) {
            return DEFAULT_TRACKED_TIMEOUT;
        }
        return trackedTimeout;
    }
}
