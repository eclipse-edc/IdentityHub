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

package org.eclipse.dataspaceconnector.identityhub.credentials;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.readString;

public class CryptoUtils {

    public static PublicKeyWrapper readPublicEcKey(String file) throws IOException, JOSEException {
        return new EcPublicKeyWrapper(readEcKeyPemFile(file));
    }

    public static PrivateKeyWrapper readPrivateEcKey(String file) throws IOException, JOSEException {
        return new EcPrivateKeyWrapper(readEcKeyPemFile(file));
    }

    private static ECKey readEcKeyPemFile(String file) throws IOException, JOSEException {
        var contents = readString(Path.of(file));
        var jwk = (ECKey) ECKey.parseFromPEMEncodedObjects(contents);
        return jwk;
    }
}
