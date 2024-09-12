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
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.identityhub.common.provisioner.StsAccountProvisionerExtension.NAME;

@Extension(value = NAME)
public class StsAccountProvisionerExtension implements ServiceExtension {
    public static final String NAME = "STS Account Provisioner Extension";

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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("STS-Account");
        if (stsClientStore != null) {
            var provisioner = new StsAccountProvisioner(monitor, keyPairService, didDocumentService, stsClientStore, vault);
            eventRouter.registerSync(ParticipantContextCreated.class, provisioner);
            eventRouter.registerSync(ParticipantContextDeleted.class, provisioner);
            eventRouter.registerSync(KeyPairAdded.class, provisioner);
            eventRouter.registerSync(KeyPairRevoked.class, provisioner);
            eventRouter.registerSync(KeyPairRotated.class, provisioner);
            eventRouter.registerSync(KeyPairActivated.class, provisioner);
        } else {
            monitor.warning("STS Client Store not available (are you using a standalone STS?). Synchronizing ParticipantContexts with STS not possible.");
        }
    }
}
