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

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.common.provisioner.StsAccountProvisionerExtension.NAME;

@Extension(value = NAME)
public class StsAccountProvisionerExtension implements ServiceExtension {
    public static final String NAME = "STS Account Provisioner Extension";
    public static final int DEFAULT_CLIENT_SECRET_LENGTH = 16;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private Vault vault;
    @Inject(required = false)
    private StsClientSecretGenerator stsClientSecretGenerator;
    @Inject
    private StsAccountService accountService;


    private StsAccountProvisionerImpl provisioner;

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
    public StsAccountProvisioner createProvisioner(ServiceExtensionContext context) {
        if (provisioner == null) {
            var monitor = context.getMonitor().withPrefix("STS-Account");
            provisioner = new StsAccountProvisionerImpl(monitor, vault, stsClientSecretGenerator(), accountService);
            eventRouter.registerSync(ParticipantContextDeleted.class, provisioner);
            eventRouter.registerSync(KeyPairRevoked.class, provisioner);
            eventRouter.registerSync(KeyPairRotated.class, provisioner);
        }
        return provisioner;
    }

    private StsClientSecretGenerator stsClientSecretGenerator() {
        return ofNullable(stsClientSecretGenerator)
                .orElseGet(RandomStringGenerator::new);
    }

}
