/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.tck.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.test.e2e.tck.attestation.TckTestAttestationsExtension.NAME;


@Extension(value = NAME)
public class TckTestAttestationsExtension implements ServiceExtension {

    public static final String NAME = "TCK Test Attestations Extension";

    @Inject
    private AttestationSourceFactoryRegistry registry;

    @Inject
    private AttestationDefinitionValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.registerFactory("tck-test", new TckAttestationSourceFactory());
        validatorRegistry.registerValidator("tck-test", new TckAttestationSourceValidator());
    }
}
