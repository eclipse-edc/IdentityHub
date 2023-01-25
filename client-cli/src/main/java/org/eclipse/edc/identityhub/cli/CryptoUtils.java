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

package org.eclipse.edc.identityhub.cli;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.readString;

public class CryptoUtils {

    private CryptoUtils() {
    }

    /**
     * Read {@link ECKey} from a PEM file.
     *
     * @throws IOException   if file cannot be read.
     * @throws JOSEException if {@link ECKey} cannot be parsed from PEM.
     */
    public static ECKey readEcKeyPemFile(String file) throws IOException, JOSEException {
        var contents = readString(Path.of(file));
        return (ECKey) ECKey.parseFromPEMEncodedObjects(contents);
    }
}
