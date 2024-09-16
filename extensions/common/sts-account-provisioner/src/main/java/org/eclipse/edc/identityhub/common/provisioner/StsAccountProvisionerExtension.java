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

import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.common.provisioner.StsAccountProvisionerExtension.NAME;

@Extension(value = NAME)
public class StsAccountProvisionerExtension implements ServiceExtension {
    public static final String NAME = "STS Account Provisioner Extension";
    public static final int DEFAULT_CLIENT_SECRET_LENGTH = 16;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private KeyPairService keyPairService;
    @Inject
    private DidDocumentService didDocumentService;
    @Inject(required = false)
    private StsClientStore stsClientStore;
    @Inject
    private Vault vault;
    @Inject(required = false)
    private StsClientSecretGenerator stsClientSecretGenerator;

    private StsAccountProvisioner provisioner;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // invoke once, so that the event registration definitely happens
        createProvisioner(context);
    }

    @Provider
    public AccountProvisioner createProvisioner(ServiceExtensionContext context) {
        if (provisioner == null) {
            var monitor = context.getMonitor().withPrefix("STS-Account");
            if (stsClientStore != null) {
                monitor.debug("This IdentityHub runtime contains an embedded SecureTokenService (STS) instance. ParticipantContexts and STS Clients will be synchronized.");
                provisioner = new StsAccountProvisioner(monitor, stsClientStore, vault, stsClientSecretGenerator());
                eventRouter.registerSync(ParticipantContextDeleted.class, provisioner);
                eventRouter.registerSync(KeyPairRevoked.class, provisioner);
                eventRouter.registerSync(KeyPairRotated.class, provisioner);
            } else {
                monitor.warning("This IdentityHub runtime does NOT contain an embedded SecureTokenService (STS) instance. " +
                        "Synchronizing ParticipantContexts and STS clients must be handled out-of-band.");
            }
        }
        return provisioner;
    }

    private StsClientSecretGenerator stsClientSecretGenerator() {
        return ofNullable(stsClientSecretGenerator)
                .orElseGet(RandomStringGenerator::new);
    }

    /**
     * Default client secret generator that creates an alpha-numeric string of length {@link StsAccountProvisionerExtension#DEFAULT_CLIENT_SECRET_LENGTH}
     * (16).
     */
    private static class RandomStringGenerator implements StsClientSecretGenerator {
        @Override
        public String generateClientSecret(@Nullable Object parameters) {
            // algorithm taken from https://www.baeldung.com/java-random-string
            int leftLimit = 48; // numeral '0'
            int rightLimit = 122; // letter 'z'
            var random = new SecureRandom();

            return random.ints(leftLimit, rightLimit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(DEFAULT_CLIENT_SECRET_LENGTH)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }
    }
}
