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
                description = "Interval in milliseconds at which the Issuer is asked about the status of credential requests that are still awaiting their credentials",
                defaultValue = DEFAULT_STATUS_POLL_INTERVAL + "", key = "edc.iam.credential.request.status.poll.interval"
        )
        long statusPollInterval,

        @Setting(
                description = "The scope that will be used to issue an access token to be included in the Self-Issued ID token in the 'token' claim. By default, no token will be generated.",
                required = false, key = "edc.iam.credential.request.bearer.access.scope"
        )
        String bearerAccessScope
) {

    public static final int DEFAULT_STATUS_POLL_INTERVAL = 5000;

    @Override
    public long statusPollInterval() {
        if (statusPollInterval == 0) {
            return DEFAULT_STATUS_POLL_INTERVAL;
        }
        return statusPollInterval;
    }
}
