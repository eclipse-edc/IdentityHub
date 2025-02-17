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

package org.eclipse.edc.identityhub.sts;

import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.identityhub.sts.accountservice.LocalStsAccountServiceExtension.NAME;


@Extension(value = NAME)
public class LocalStsServiceExtension implements ServiceExtension {
    public static final String NAME = "Local (embedded) STS Account Service Extension";
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;
    @Inject
    private Clock clock;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private JwsSignerProvider externalSigner;

    @Setting(description = "Alias of private key used for signing tokens, retrieved from private key resolver. Required when using Embedded STS", key = "edc.iam.sts.privatekey.alias", required = false)
    private String privateKeyAlias;
    @Setting(description = "Key Identifier used by the counterparty to resolve the public key for token validation, e.g. did:example:123#public-key-1. Required when using Embedded STS", key = "edc.iam.sts.publickey.id", required = false)
    private String publicKeyId;
    @Setting(description = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + DEFAULT_STS_TOKEN_EXPIRATION_MIN, key = "edc.iam.sts.token.expiration")
    private long stsTokenExpirationMin;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenService createDefaultTokenService(ServiceExtensionContext context) {
        context.getMonitor().info("Using the Embedded STS client, as no other implementation was provided.");

        if (context.getSetting("edc.oauth.token.url", null) != null) {
            context.getMonitor().warning("The property '%s' was configured, but no remote SecureTokenService was found on the classpath. ".formatted("edc.oauth.token.url") +
                    "This could be an indication of a configuration problem.");
        }

        return new EmbeddedSecureTokenService(new JwtGenerationService(externalSigner), () -> privateKeyAlias, () -> publicKeyId, clock, TimeUnit.MINUTES.toSeconds(stsTokenExpirationMin), jtiValidationStore);
    }
}
