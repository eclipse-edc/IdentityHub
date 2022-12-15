/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.processor.data;

import org.eclipse.edc.identityhub.spi.processor.data.DataValidator;
import org.eclipse.edc.spi.result.Result;

public class LdpVerifiableCredential implements DataValidator {

    public static final String DATA_FORMAT = "application/vc+ldp";

    @Override
    public Result<Void> validate(byte[] data) {
        return null;
    }

    @Override
    public String dataFormat() {
        return DATA_FORMAT;
    }
}
