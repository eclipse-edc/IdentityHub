/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.common.provisioner;

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

/**
 * Default client secret generator that creates an alpha-numeric string of length {@link StsAccountProvisionerExtension#DEFAULT_CLIENT_SECRET_LENGTH}
 * (16).
 */
class RandomStringGenerator implements StsClientSecretGenerator {
    @Override
    public String generateClientSecret(@Nullable Object parameters) {
        // algorithm taken from https://www.baeldung.com/java-random-string
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        var random = new SecureRandom();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(StsAccountProvisionerExtension.DEFAULT_CLIENT_SECRET_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
