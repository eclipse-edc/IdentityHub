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

package org.eclipse.edc.issuerservice.issuance.attestations;

import org.eclipse.edc.issuerservice.issuance.attestations.presentation.PresentationAttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.issuerservice.issuance.attestations.IssuanceAttestationsExtension.NAME;

@Extension(NAME)
public class IssuanceAttestationsExtension implements ServiceExtension {

    public static final String NAME = "Issuance Attestations Extension";

    @Inject
    private AttestationSourceFactoryRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.registerFactory("presentation", new PresentationAttestationSourceFactory());
    }
}
