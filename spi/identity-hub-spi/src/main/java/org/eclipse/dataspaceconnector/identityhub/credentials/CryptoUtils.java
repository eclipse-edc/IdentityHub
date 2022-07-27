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
        var contents = readString(Path.of(file));
        var jwk = (ECKey) ECKey.parseFromPEMEncodedObjects(contents);
        return new EcPublicKeyWrapper(jwk);
    }

    public static PrivateKeyWrapper readPrivateEcKey(String file) throws IOException, JOSEException {
        var contents = readString(Path.of(file));
        var jwk = (ECKey) ECKey.parseFromPEMEncodedObjects(contents);
        return new EcPrivateKeyWrapper(jwk);
    }
}
